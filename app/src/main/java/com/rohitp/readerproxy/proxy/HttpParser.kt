package com.rohitp.readerproxy.proxy

import androidx.core.net.toUri
import java.io.BufferedReader

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
}
