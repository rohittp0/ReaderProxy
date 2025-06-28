package com.rohitp.readerproxy.proxy

import android.net.VpnService
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class PlainChannel(vpn: VpnService, host: String, port: Int) {
    val socket: Socket = Socket().apply {
        vpn.protect(this)
        tcpNoDelay = true
        connect(InetSocketAddress(host, port))
    }
}

class TlsChannel(
    vpn: VpnService,
    host: String,
    port: Int,
    browser: Socket,               // ← NEW
    clientCtx: SSLContext
) {
    /** TLS layer towards the browser (forged cert). */
    val clientSock = (clientCtx.socketFactory.createSocket(browser, host, port, false) as SSLSocket)
            .apply { useClientMode = false; startHandshake() }

    /** TLS layer towards the origin server. */
    val serverSock = run {
        (SSLSocketFactory.getDefault()
            .createSocket(host, port) as SSLSocket)
            .also {
                vpn.protect(it) // protect the socket from VPN
                val p = it.sslParameters
                p.serverNames = listOf(SNIHostName(host)) // SNI fix
                it.sslParameters = p
                it.startHandshake()
            }
    }
}

