package com.rohitp.readerproxy.proxy

import android.net.VpnService
import com.rohitp.readerproxy.logic.HtmlProcessor
import com.rohitp.readerproxy.markAndPeekLine
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket

class ClientWorker(
    private val vpn: VpnService,
    private val browser: Socket,
    private val certMgr: CertManager,
    private val html: HtmlProcessor
) : Runnable {

    override fun run() {
        try {
            val buf = BufferedInputStream(browser.getInputStream())
            val firstLine = buf.markAndPeekLine()
            if (firstLine.startsWith("CONNECT", true)) handleConnect(buf, firstLine)
            else handlePlain(buf)
        } catch (e: Exception) {
            Timber.e(e, "worker crashed")
        } finally { browser.close() }
    }

    private fun handlePlain(buf: BufferedInputStream) {
        val reader = BufferedReader(InputStreamReader(buf))
        val req    = HttpParser.parseRequest(reader) ?: return
        PlainChannel(vpn, req.host, req.port).socket.use { origin ->
            HttpSession(
                buf, browser.getOutputStream(),
                BufferedInputStream(origin.getInputStream()), origin.getOutputStream(),
                html
            ).relayOnce()
        }
    }

    private fun handleConnect(buf: BufferedInputStream, first: String) {
        val host = first.split(' ')[1].substringBefore(':')
        val port = first.split(' ')[1].substringAfter(':').toInt()
        browser.getOutputStream().write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())

        val tls = TlsChannel(vpn, host, port, browser, certMgr.serverContext(host))

        tls.clientSock.use { cli ->
            HttpSession(
                BufferedInputStream(cli.getInputStream()), cli.getOutputStream(),
                BufferedInputStream(tls.serverSock.getInputStream()), tls.serverSock.getOutputStream(),
                html
            ).relayOnce()                // single request, close afterward
        }
    }
}
