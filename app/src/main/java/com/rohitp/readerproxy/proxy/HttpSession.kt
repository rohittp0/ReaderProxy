package com.rohitp.readerproxy.proxy

import com.rohitp.readerproxy.logic.HtmlProcessor
import com.rohitp.readerproxy.maybeGunzip
import com.rohitp.readerproxy.readChunked
import com.rohitp.readerproxy.readLineAscii
import java.io.BufferedInputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

enum class Protocol(val value: String) {
    HTTP("http"), HTTPS("https");
}

/**
 * Handles ONE request–response cycle on already-connected streams.
 * Works for plain-text or TLS because it only uses Input/OutputStreams.
 */
class HttpSession(
    private val clientIn: BufferedInputStream,
    private val clientOut: OutputStream,
    private val serverIn: BufferedInputStream,
    private val serverOut: OutputStream,
    private val htmlProcessor: HtmlProcessor
) {

    private fun getFilteredRequestHeaders(): List<String> {
        return readRequestHeaders().filterNot {
            it.startsWith("Accept-Encoding", true) ||
                    it.startsWith("Proxy-Connection", true) ||
                    it.startsWith("Connection", true)
        }
            .plus("Accept-Encoding: gzip, identity")
            .plus("Connection: close")

    }

    private fun readRequestHeaders(): List<String> {
        val headers = mutableListOf<String>()
        while (true) {
            val line = clientIn.readLineAscii()
            if (line.isEmpty()) break
            headers += line
        }
        return headers
    }

    private fun respondToClient(statusLine: String, respHdrs: List<String>, body: ByteArray) {
        val sb = StringBuilder()

        sb.append(statusLine).append("\r\n")
        respHdrs.forEach { sb.append(it).append("\r\n") }
        sb.append("\r\n")

        clientOut.write(sb.toString().toByteArray(StandardCharsets.US_ASCII))
        clientOut.write(body)
        clientOut.flush()
    }

    private fun readResponseHeaders(): List<String> {
        val headers = mutableListOf<String>()
        while (true) {
            val line = serverIn.readLineAscii()
            if (line.isEmpty()) break
            headers += line
        }
        return headers
    }

    /** returns true if connection may stay open (keep-alive) */
    fun relayOnce(protocol: Protocol, host: String): Boolean {
        /* ----- read req ----- */
        val reqLine = clientIn.readLineAscii()
        if (reqLine.isEmpty()) return false
        // no body support yet (GET/HEAD is fine)

        /* ----- forward req ----- */
        serverOut.write("$reqLine\r\n".toByteArray())

        getFilteredRequestHeaders().forEach { serverOut.write("$it\r\n".toByteArray()) }
        serverOut.write("\r\n".toByteArray())
        serverOut.flush()

        /* ----- read resp hdr ----- */
        val statusLine = serverIn.readLineAscii()
        val respHdrs = readResponseHeaders()

        var isChunked = false
        var isGzip = false
        var isHtml = false
        var keepAlive = false

        val modifiedRespHdrs = mutableListOf<String>()

        respHdrs.forEach { h ->
            val l = h.lowercase()
            when {
                l.startsWith("transfer-encoding:") && l.contains("chunked") -> {
                    isChunked = true
                    return@forEach
                }
                l.startsWith("content-encoding:") && l.contains("gzip") -> {
                    isGzip = true
                    return@forEach
                }
                l.startsWith("connection:") ->  {
                    keepAlive = l.contains("keep-alive")
                    return@forEach
                }
                l.startsWith("content-type:") && l.contains("text/html") -> isHtml = true
            }
            if (!l.startsWith("content-length:")) modifiedRespHdrs += h
        }

        if(!isHtml) {
            respondToClient(statusLine, respHdrs, serverIn.readBytes())
            return false
        }

        /* ----- read resp body ----- */
        val rawBody = if (!isChunked) serverIn.readBytes() else serverIn.readChunked()
        val bodyForClient = htmlProcessor.process(
            String(rawBody.maybeGunzip(), Charsets.UTF_8),
            protocol,
            host,
            reqLine
        )
            .toByteArray(Charsets.UTF_8)

        modifiedRespHdrs += "Content-Length: ${bodyForClient.size}"
        modifiedRespHdrs += "Content-Encoding: identity"
        modifiedRespHdrs += "Connection: close"

        respondToClient(statusLine, modifiedRespHdrs, bodyForClient)

        return keepAlive
    }
}
