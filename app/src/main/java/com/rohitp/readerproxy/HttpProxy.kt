package com.rohitp.readerproxy

import android.net.VpnService
import androidx.core.net.toUri
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class HttpProxy(private val vpn: VpnService, private val listenPort: Int = 8888) {

    private val executor = Executors.newCachedThreadPool()

    fun start() {
        val server = ServerSocket(listenPort)
        Timber.i("Starting HTTP proxy on port $listenPort")

        while (!server.isClosed) {
            val client = server.accept()
            executor.execute { handleClient(client) }
        }
    }

    private fun getProtectedSocket(): Socket {
        val upstream = Socket()
        vpn.protect(upstream)                       // extra safety, no harm

        return upstream
    }

    private fun handleClient(client: Socket) {
        client.soTimeout = 15_000
        try {
            val reader = BufferedReader(
                InputStreamReader(
                    client.getInputStream(),
                    StandardCharsets.US_ASCII
                )
            )

            /* ---------- 1. Parse the request line ---------- */
            val firstLine = reader.readLine() ?: return  // e.g.  "GET http://example.com/ HTTP/1.1"
            val parts = firstLine.split(' ')
            if (parts.size < 3) return
            val method = parts[0]
            val absUri = parts[1].toUri()
            val version = parts[2]

            /* ---------- 2. Read headers ---------- */
            val headerLines = mutableListOf<String>()
            var hostHeader: String? = null
            var line: String
            while (reader.readLine().also { line = it ?: "" }.isNotEmpty()) {
                headerLines += line
                if (line.startsWith("Host:", ignoreCase = true))
                    hostHeader = line.substringAfter(':').trim()
            }

            // We only support simple GET/POST without a body for now
            val targetHostPort = hostHeader ?: absUri.authority ?: return
            val (host, port) = run {
                val split = targetHostPort.split(':')
                split[0] to (if (split.size > 1) split[1].toInt() else 80)
            }

            if (port == 443)
                return Timber.w("HTTPS requests are not supported: $absUri")

            Timber.i("Handling request: $method $absUri to $host:$port")

            /* ---------- 3. Build request line & headers ---------- */
            val pathOnly = buildString {
                append(absUri.encodedPath ?: "/")
                absUri.encodedQuery?.let { append('?').append(it) }
            }
            val newRequestLine = "$method $pathOnly $version\r\n"

// we’ll rebuild headers, ensuring we send Connection: close
            val filtered = headerLines
                .filterNot { it.startsWith("Proxy-Connection", true) || it.startsWith("Connection", true) }
            val finalHeaders = buildString {
                filtered.forEach { append(it).append("\r\n") }
                append("Connection: close\r\n\r\n")   // force server to close after body
            }

            /* ---------- 4. Connect & send ---------- */
            val upstream = getProtectedSocket()
            upstream.tcpNoDelay = true
            upstream.connect(InetSocketAddress(host, port), 10_000)

            val upstreamOut = BufferedOutputStream(upstream.getOutputStream())
            val upstreamIn  = BufferedInputStream(upstream.getInputStream())

            upstreamOut.write(newRequestLine.toByteArray())
            upstreamOut.write(finalHeaders.toByteArray())
            upstreamOut.flush()

            Timber.d("Request sent to upstream server")

            /* ---------- 5. Stream response ---------- */
            val clientOut = client.getOutputStream()            // unbuffered is fine
            val buf = ByteArray(16 * 1024)

            while (true) {
                val n = upstreamIn.read(buf)
                if (n < 0) break
                clientOut.write(buf, 0, n)   // immediate push
            }
            clientOut.flush()

            Timber.d("Response sent to client")
        } catch (e: Exception) {
            Timber.e(e, "Error handling client request")
        } finally {
            try {
                client.close()
            } catch (_: IOException) {
            }
        }
    }
}