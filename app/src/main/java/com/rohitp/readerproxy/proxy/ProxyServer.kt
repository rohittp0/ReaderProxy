package com.rohitp.readerproxy.proxy

import android.net.VpnService
import com.rohitp.readerproxy.logic.HtmlProcessor
import timber.log.Timber
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.Executors

class ProxyServer(
    private val vpn: VpnService,
    private val port: Int = 8888
) {

    private val pool = Executors.newCachedThreadPool()
    private val certMgr = CertManager(vpn)
    private val htmlProcessor = HtmlProcessor()
    private var serverSocket: ServerSocket? = null

    fun startBlocking() {
        serverSocket = ServerSocket(port)
        serverSocket?.use { server ->
            Timber.i("HTTP proxy listening on $port")
            try {
                while (true) {
                    val socket = server.accept()
                    pool.execute(ClientWorker(vpn, socket, certMgr, htmlProcessor))
                }
            }
            catch (_: SocketException){}
        }
    }

    fun stop() {
        Timber.i("Stopping HTTP proxy server")
        pool.shutdownNow()
        serverSocket?.close()
        Timber.i("HTTP proxy server stopped")
    }
}
