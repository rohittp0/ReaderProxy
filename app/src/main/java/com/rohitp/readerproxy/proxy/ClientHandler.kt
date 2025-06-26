package com.rohitp.readerproxy.proxy

import android.net.VpnService
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets

class ClientHandler(
    private val vpn: VpnService,
    private val client: Socket
) : Runnable {

    companion object {
        private const val CONNECT_TIMEOUT_MS = 10_000
        private val COPY_BUF = ByteArray(16 * 1024)
    }

    override fun run() {
        client.soTimeout = 15_000
        client.use { cli ->
            val reader = BufferedReader(InputStreamReader(cli.getInputStream(), StandardCharsets.US_ASCII))

            /** ---------- Parse request ---------- */
            val req = HttpParser.parseRequest(reader) ?: return
            if (req.port == 443) {
                Timber.w("TODO HTTPS: ${req.uri}")
                return   // ← stub, will handle CONNECT later
            }

            /** ---------- Upstream socket ---------- */
            val upstream = createUpstreamSocket(req.host, req.port) ?: return
            upstream.use { up ->
                val upOut = BufferedOutputStream(up.getOutputStream())
                val upIn  = BufferedInputStream(up.getInputStream())

                HttpParser.writeRequest(req, upOut)   // send to origin
                Timber.d("→ ${req.method} ${req.host}")

                /** ---------- Copy origin → client ---------- */
                val cliOut = cli.getOutputStream()
                while (true) {
                    val n = upIn.read(COPY_BUF)
                    if (n < 0) break
                    cliOut.write(COPY_BUF, 0, n)
                }
                cliOut.flush()
                Timber.d("← response ${req.host} (${req.method})")
            }
        }
    }

    /** Protected, non-VPN socket. */
    private fun createUpstreamSocket(host: String, port: Int): Socket? = try {
        Socket().apply {
            vpn.protect(this)                 // bypass VPN tunnel
            tcpNoDelay = true
            connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
        }
    } catch (e: IOException) {
        Timber.e(e, "Unable to connect to $host:$port")
        null
    }
}
