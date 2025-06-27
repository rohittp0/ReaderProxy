package com.rohitp.readerproxy.proxy

import android.net.VpnService
import com.rohitp.readerproxy.logic.HtmlProcessor
import com.rohitp.readerproxy.markAndPeekLine
import com.rohitp.readerproxy.readLineAscii
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

private class IOThread(private val inp: InputStream, private val out: OutputStream) : Thread() {
    override fun run() { inp.copyTo(out); out.flush() }
}


class ClientWorker(
    private val vpn: VpnService,
    private val client: Socket,
    private val certMgr: CertManager,
    private val htmlProc: HtmlProcessor
) : Runnable {

    override fun run() {
        client.soTimeout = 20_000
        client.use { cli ->
            val inBuf  = BufferedInputStream(cli.getInputStream())
            val peek   = inBuf.markAndPeekLine()
            if (peek.startsWith("CONNECT", true)) handleHttpsTunnel(cli, peek)
            else handlePlainHttp(cli, BufferedReader(InputStreamReader(inBuf)))
        }
    }

    /** -------- plain HTTP -------- */
    private fun handlePlainHttp(cli: Socket, reader: BufferedReader) {
        val req = HttpParser.parseRequest(reader) ?: return
        val upstream = protectedSocket(req.host, req.port) ?: return
        upstream.use { up ->
            // forward request
            HttpParser.writeRequest(req, up.getOutputStream())

            val upIn  = BufferedInputStream(up.getInputStream())
            val cliOut = cli.getOutputStream()

            // read response headers first
            val headers = StringBuilder()
            while (true) {
                val line = upIn.readLineAscii()
                if (line.isEmpty()) break
                headers.append(line).append("\r\n")
            }
            headers.append("\r\n")
            val hdrBytes = headers.toString().toByteArray(StandardCharsets.US_ASCII)
            cliOut.write(hdrBytes)

            // detect HTML
            val isHtml = headers.contains("Content-Type:", true) &&
                    headers.contains("text/html", true)

            if (!isHtml) {
                upIn.copyTo(cliOut)                 // stream verbatim
            } else {
                val body = upIn.readBytes()
                val modified = htmlProc.process(String(body, StandardCharsets.UTF_8))
                cliOut.write(modified.toByteArray(StandardCharsets.UTF_8))
            }
            cliOut.flush()
        }
    }

    /** -------- HTTPS via CONNECT -------- */
    private fun handleHttpsTunnel(cli: Socket, firstLine: String) {
        val hostPort = firstLine.split(' ')[1]
        val host = hostPort.substringBefore(':')
        val port = hostPort.substringAfter(':').toInt()

        // 1) Acknowledge CONNECT
        cli.getOutputStream().write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())

        // 2) TLS to client with forged cert
        val sslCtx = certMgr.serverContext(host)
        val sslToClient = sslCtx.socketFactory
            .createSocket(cli, cli.inetAddress.hostAddress, cli.port, /*autoClose*/false) as SSLSocket
        sslToClient.useClientMode = false
        sslToClient.startHandshake()

        // 3) TLS to origin
        val upRaw   = protectedSocket(host, port) ?: return
        val upSsl   = (SSLSocketFactory.getDefault() as SSLSocketFactory)
            .createSocket(upRaw, host, port, true) as SSLSocket
        upSsl.startHandshake()

        // 4) Bridge both directions
        val down = IOThread(sslToClient.inputStream, upSsl.outputStream)
        val up   = IOThread(upSsl.inputStream, sslToClient.outputStream)
        down.start(); up.start()
        down.join();  up.join()
    }

    /** Helper: create socket bypassing VPN */
    private fun protectedSocket(host: String, port: Int): Socket? = try {
        Socket().apply {
            vpn.protect(this)                 // bypass VPN tunnel
            tcpNoDelay = true
            connect(InetSocketAddress(host, port))
        }
    } catch (e: Exception) {
        Timber.e(e, "Connect fail $host:$port")
        null
    }
}
