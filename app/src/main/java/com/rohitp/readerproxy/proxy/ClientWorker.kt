package com.rohitp.readerproxy.proxy

import android.net.VpnService
import com.rohitp.readerproxy.logic.HtmlProcessor
import com.rohitp.readerproxy.markAndPeekLine
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import javax.net.ssl.SSLHandshakeException

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
            try {
                if (firstLine.startsWith("CONNECT", true)) handleConnect(firstLine)
                else handlePlain(buf)
            }
            catch (_: SSLHandshakeException) {
                Timber.w("TLS handshake failed, trying plain HTTP")
                handlePlain(buf)
            }
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
            ).relayOnce(Protocol.HTTP, req.host)
        }
    }

    private fun handleConnect(first: String) {
        val host = first.split(' ')[1].substringBefore(':')
        val port = first.split(' ')[1].substringAfter(':').toInt()
        browser.getOutputStream().write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())

        val tls = TlsChannel(vpn, host, port, browser, certMgr.serverContext(host))

        tls.clientSock.use { cli ->
            HttpSession(
                BufferedInputStream(cli.getInputStream()), cli.getOutputStream(),
                BufferedInputStream(tls.serverSock.getInputStream()), tls.serverSock.getOutputStream(),
                html
            ).relayOnce(Protocol.HTTPS, host)                // single request, close afterward
        }
    }
}
