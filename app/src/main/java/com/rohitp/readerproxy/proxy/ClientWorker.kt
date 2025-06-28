package com.rohitp.readerproxy.proxy

import android.net.VpnService
import com.rohitp.readerproxy.logic.HtmlProcessor
import com.rohitp.readerproxy.markAndPeekLine
import com.rohitp.readerproxy.maybeGunzip
import com.rohitp.readerproxy.readChunked
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
    override fun run() {
        inp.copyTo(out); out.flush()
    }
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
            val inBuf = BufferedInputStream(cli.getInputStream())
            val peek = inBuf.markAndPeekLine()
            if (peek.startsWith("CONNECT", true)) handleHttpsTunnel(cli, peek)
            else handlePlainHttp(cli, BufferedReader(InputStreamReader(inBuf)))
        }
    }

    private fun handlePlainHttp(cli: Socket, reader: BufferedReader) {
        val req = HttpParser.parseRequest(reader) ?: return
        val upstream = protectedSocket(req.host, req.port) ?: return
        upstream.use { up ->
            HttpParser.writeRequest(req, up.getOutputStream())

            val upIn = BufferedInputStream(up.getInputStream())
            val cliOut = cli.getOutputStream()

            val rebuiltHeaders = StringBuilder()
            val originalHeaders = StringBuilder()

            var isGzip = false
            var isHtml = false
            var isChunked = false

            while (true) {
                val line = upIn.readLineAscii()
                if (line.isEmpty()) break

                val lower = line.lowercase()

                when (lower) {
                    "transfer-encoding: chunked" -> isChunked = true
                    "content-encoding: gzip" -> isGzip = true
                    "content-type: text/html" -> isHtml = true
                    "connection: close" -> {}
                    else -> if (!lower.startsWith("content-length:"))
                        rebuiltHeaders.append(line).append("\r\n")
                }
                originalHeaders.append(line).append("\r\n")
            }

            if (!isHtml) {
                cliOut.write(originalHeaders.toString().toByteArray(StandardCharsets.US_ASCII))
                cliOut.write(upIn.readBytes())
                cliOut.flush()
                return
            }

            val rawBody = if (!isChunked) upIn.readBytes()                       // simple
            else upIn.readChunked()                    // de-chunk first

            val body = rawBody.maybeGunzip()
            val processedBody = htmlProc.process(String(body, StandardCharsets.UTF_8))
                .toByteArray(StandardCharsets.UTF_8)

            /** --- write back --- */
            rebuiltHeaders
                .append("Content-Length: ${processedBody.size}\r\n")
                .append("Content-Type: text/html\r\n")
                .append("Connection: close\r\n\r\n")

            cliOut.write(rebuiltHeaders.toString().toByteArray(StandardCharsets.US_ASCII))
            cliOut.write(processedBody)
            cliOut.flush()
        }
    }

    /** -------- HTTPS via CONNECT -------- */
    private fun handleHttpsTunnel(cli: Socket, firstLine: String) {
        return
        val hostPort = firstLine.split(' ')[1]
        val host = hostPort.substringBefore(':')
        val port = hostPort.substringAfter(':').toInt()

        // 1) Acknowledge CONNECT
        cli.getOutputStream()
            .write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())

        // 2) TLS *to browser* with forged cert
        val sslToClient = certMgr
            .serverContext(host)
            .socketFactory
            .createSocket(cli, host, port, /*autoClose*/false) as SSLSocket
        sslToClient.useClientMode = false
        sslToClient.startHandshake()

        // 3) TLS *to origin*
        val upTcp = protectedSocket(host, port) ?: return
        val upSsl = (SSLSocketFactory.getDefault() as SSLSocketFactory)
            .createSocket(upTcp, host, port, true) as SSLSocket
        upSsl.startHandshake()

        // 4) Bi-directional pipe (HTML interception TODO)
        IOThread(sslToClient.inputStream, upSsl.outputStream).start()
        val copy = IOThread(upSsl.inputStream, sslToClient.outputStream)
        copy.start()
        copy.join()
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
