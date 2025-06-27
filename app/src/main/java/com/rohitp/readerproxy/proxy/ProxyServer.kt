package com.rohitp.readerproxy.proxy

import android.net.VpnService
import timber.log.Timber
import java.net.ServerSocket
import java.util.concurrent.Executors

class ProxyServer(
    private val vpn: VpnService,
    private val port: Int = 8888,
    threadPoolSize: Int = 16
) {

    private val executor = Executors.newFixedThreadPool(threadPoolSize)

    fun startBlocking() {
        ServerSocket(port).use { server ->
            Timber.i("HTTP proxy listening on $port")
            while (!server.isClosed) {
                val client = server.accept()
                executor.execute { ClientHandler(vpn, client).run() }
            }
        }
    }
}
