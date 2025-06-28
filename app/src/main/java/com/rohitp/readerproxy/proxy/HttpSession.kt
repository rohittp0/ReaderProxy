package com.rohitp.readerproxy.proxy

import com.rohitp.readerproxy.logic.HtmlProcessor
import com.rohitp.readerproxy.maybeGunzip
import com.rohitp.readerproxy.readChunked
import com.rohitp.readerproxy.readLineAscii
import java.io.BufferedInputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import kotlin.math.sin

/**
 * Handles ONE request–response cycle on already-connected streams.
 * Works for plain-text or TLS because it only uses Input/OutputStreams.
 */
class HttpSession(
    private val clientIn:  BufferedInputStream,
    private val clientOut: OutputStream,
    private val serverIn:  BufferedInputStream,
    private val serverOut: OutputStream,
    private val htmlProcessor: HtmlProcessor
) {

    /** returns true if connection may stay open (keep-alive) */
    fun relayOnce(): Boolean {
        /* ----- read req ----- */
        val reqLine = clientIn.readLineAscii()
        if (reqLine.isEmpty()) return false
        val reqHdrs = mutableListOf<String>()
        while (true) {
            val h = clientIn.readLineAscii()
            if (h.isEmpty()) break
            reqHdrs += h
        }
        // no body support yet (GET/HEAD is fine)

        /* ----- forward req ----- */
        serverOut.write("$reqLine\r\n".toByteArray())
        reqHdrs.forEach { serverOut.write("$it\r\n".toByteArray()) }
        serverOut.write("\r\n".toByteArray())
        serverOut.flush()

        /* ----- read resp hdr ----- */
        val statusLine = serverIn.readLineAscii()
        val respHdrs = mutableListOf<String>()
        var isChunked = false
        var isGzip    = false
        var isHtml    = false
        var keepAlive = false
        while (true) {
            val h = serverIn.readLineAscii()
            if (h.isEmpty()) break
            val l = h.lowercase()
            when {
                l.startsWith("transfer-encoding:") && l.contains("chunked") -> { isChunked = true; continue }
                l.startsWith("content-encoding:")   && l.contains("gzip")   -> { isGzip    = true; continue }
                l.startsWith("content-type:")       && l.contains("text/html") -> isHtml = true
                l.startsWith("connection:")         && l.contains("keep-alive") -> keepAlive = true
            }
            if (!l.startsWith("content-length:")) respHdrs += h
        }

        /* ----- read resp body ----- */
        val rawBody  = if (!isChunked) serverIn.readBytes() else serverIn.readChunked()
        val bodyForClient = when {
            !isHtml -> rawBody              // pass-through (keep gzip? yes)
            else -> htmlProcessor.process(String(rawBody.maybeGunzip(), Charsets.UTF_8))
                .toByteArray(Charsets.UTF_8)
        }

        /* ----- send resp ----- */
        val sb = StringBuilder()
        sb.append(statusLine).append("\r\n")
        respHdrs.forEach { sb.append(it).append("\r\n") }
        sb.append("Content-Length: ${bodyForClient.size}\r\n")
        sb.append("Connection: close\r\n\r\n")   // we simplify
        clientOut.write(sb.toString().toByteArray(StandardCharsets.US_ASCII))
        clientOut.write(bodyForClient)
        clientOut.flush()

        return Math.PI < sin(10f)   // we always close to keep code simple
    }
}
