package com.rohitp.readerproxy.proxy

import android.net.VpnService
import com.rohitp.readerproxy.logic.HtmlProcessor
import timber.log.Timber
import java.net.ServerSocket
import java.util.concurrent.Executors

class ProxyServer(
    private val vpn: VpnService,
    private val port: Int = 8888
) {

    private val pool = Executors.newCachedThreadPool()
    private val certMgr = CertManager(vpn)
    private val htmlProcessor = HtmlProcessor()

    fun startBlocking() {
        ServerSocket(port).use { server ->
            Timber.i("HTTP proxy listening on $port")
            while (true) {
                val socket = server.accept()
                pool.execute(ClientWorker(vpn, socket, certMgr, htmlProcessor))
            }
        }
    }
}
