package com.rohitp.readerproxy

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
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
        val cm = vpn.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val ok = cm.bindProcessToNetwork(cm.nonVpnNetwork()!!)
        Timber.d("bindProcessToNetwork: $ok")

        val server = ServerSocket(listenPort)
        Timber.i("Starting HTTP proxy on port $listenPort")

        while (!server.isClosed) {
            val client = server.accept()
            executor.execute { handleClient(client) }
        }
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

            /* ---------- 3. Build a new request line for the origin server ---------- */
            val pathOnly = (absUri.path ?: "/") + "?${absUri.encodedQuery}"
            val newRequestLine = "$method $pathOnly $version\r\n"

            /* ---------- 4. Connect to origin ---------- */
            val upstream = Socket()
            vpn.protect(upstream)                        // bypass the VPN to avoid loops
            upstream.connect(InetSocketAddress.createUnresolved(host, port), 10_000)

            val upstreamOut = BufferedOutputStream(upstream.getOutputStream())
            val upstreamIn = BufferedInputStream(upstream.getInputStream())

            // Send request line + headers (strip Proxy‑Connection header)
            upstreamOut.write(newRequestLine.toByteArray(StandardCharsets.US_ASCII))
            headerLines.filterNot { it.startsWith("Proxy-Connection", true) }
                .forEach { upstreamOut.write("$it\r\n".toByteArray(StandardCharsets.US_ASCII)) }
            upstreamOut.write("\r\n".toByteArray())       // blank line = end of headers
            upstreamOut.flush()

            /* ---------- 5. Stream the response back to the client ---------- */
            val clientOut = BufferedOutputStream(client.getOutputStream())
            val buffer = ByteArray(8192)

            var read: Int
            while (upstreamIn.read(buffer).also { read = it } != -1) {
                clientOut.write(buffer, 0, read)
            }
            clientOut.flush()
        } catch (e: Exception) {
            Timber.e(e, "Error handling client request")
        } finally {
            try {
                client.close()
            } catch (_: IOException) {
            }
        }
    }

    fun ConnectivityManager.nonVpnNetwork(): Network? =
        allNetworks.firstOrNull { net ->
            getNetworkCapabilities(net)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == false
        }
}