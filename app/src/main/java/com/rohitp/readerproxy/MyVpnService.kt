package com.rohitp.readerproxy

import android.content.Intent
import android.net.ProxyInfo
import android.net.VpnService
import android.os.ParcelFileDescriptor
import kotlin.concurrent.thread

class MyVpnService : VpnService() {

    companion object {
        private const val VPN_ADDRESS = "10.10.0.2"
        private const val VPN_PREFIX  = 32           // /32 single host
        private const val PROXY_PORT  = 8888
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var proxyThread: Thread? = null

    override fun onCreate() {
        super.onCreate()
        // Start the local proxy inside a background thread
        proxyThread = thread(name = "local-http-proxy") {
            HttpProxy(this@MyVpnService, PROXY_PORT).start()
        }
    }

    override fun onStartCommand(i: Intent?, f: Int, s: Int): Int {
        if (vpnInterface == null) establishVpn()
        return START_STICKY
    }

    private fun establishVpn() {
        val builder = Builder()
            .setSession("ReaderModeVPN")
            .addAddress(VPN_ADDRESS, VPN_PREFIX)
            .addDnsServer("8.8.8.8")
            .addRoute("0.0.0.0", 0)          // capture all IPv4

        val proxyInfo = ProxyInfo.buildDirectProxy("127.0.0.1", PROXY_PORT)
        builder.setHttpProxy(proxyInfo)

        vpnInterface = builder.establish()
        // No packet parsing needed for HTTP‑only because the OS will send HTTP through the proxy.
    }

    override fun onDestroy() {
        proxyThread?.interrupt()
        vpnInterface?.close()
        super.onDestroy()
    }
}
