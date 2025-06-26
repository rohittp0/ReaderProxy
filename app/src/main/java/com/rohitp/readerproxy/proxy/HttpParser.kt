package com.rohitp.readerproxy.proxy

import androidx.core.net.toUri
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

/** One very small immutable representation of a request. */
data class HttpRequest(
    val method: String,
    val uri: String,
    val version: String,
    val host: String,
    val port: Int,
    val headers: List<String>
)

object HttpParser {

    /** Parse first line + headers; body unsupported (yet). */
    fun parseRequest(reader: BufferedReader): HttpRequest? {
        val requestLine = reader.readLine() ?: return null
        val parts = requestLine.split(' ')
        if (parts.size < 3) return null

        val method = parts[0]
        val absUri = parts[1].toUri()
        val version = parts[2]

        val headers = mutableListOf<String>()
        var hostHdr: String? = null
        while (true) {
            val line = reader.readLine() ?: ""
            if (line.isEmpty()) break
            headers += line
            if (line.startsWith("Host:", true))
                hostHdr = line.substringAfter(':').trim()
        }
        val authority = hostHdr ?: absUri.authority ?: return null
        val (host, port) = authority.split(':').let { it[0] to (it.getOrNull(1)?.toInt() ?: 80) }
        return HttpRequest(method, absUri.toString(), version, host, port, headers)
    }

    /** Forward request but always set Connection: close. */
    fun writeRequest(req: HttpRequest, writer: java.io.OutputStream) {
        val buf = BufferedWriter(OutputStreamWriter(writer, StandardCharsets.US_ASCII))
        val path = req.uri.toUri().let {
            buildString {
                append(it.encodedPath ?: "/")
                it.encodedQuery?.let { q -> append('?').append(q) }
            }
        }
        buf.append("${req.method} $path ${req.version}\r\n")
        req.headers
            .filterNot { it.startsWith("Proxy-Connection", true) || it.startsWith("Connection", true) }
            .forEach { buf.append(it).append("\r\n") }
        buf.append("Connection: close\r\n\r\n")
        buf.flush()
    }
}
