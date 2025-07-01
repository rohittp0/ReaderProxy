package com.rohitp.readerproxy

import android.os.Build
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream

private const val CR = '\r'.code

/** De-chunk Transfer-Encoding: chunked into a raw byte[] */
fun InputStream.readChunked(): ByteArray {
    val out = ByteArrayOutputStream()
    while (true) {
        // read chunk-size line (hex)
        val sizeLine = buildString {
            var c: Int
            while (read().also { c = it } != -1 && c != CR) append(c.toChar())
        }
        read() // skip LF
        val chunkSize = sizeLine.trim().toInt(16)
        if (chunkSize == 0) break
        val buf = ByteArray(chunkSize)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            readNBytes(buf, 0, chunkSize)
        }
        else {
            var bytesRead = 0
            while (bytesRead < chunkSize) {
                val readCount = read(buf, bytesRead, chunkSize - bytesRead)
                if (readCount == -1) throw IllegalStateException("Unexpected end of stream")
                bytesRead += readCount
            }
        }
        out.write(buf)
        read(); read()              // skip CR LF after the chunk
    }
    return out.toByteArray()
}

/** Inflate if the payload starts with the gzip magic; otherwise return as is. */
fun ByteArray.maybeGunzip(): ByteArray =
    if (size >= 2 && this[0] == 0x1F.toByte() && this[1] == 0x8B.toByte()) {
        GZIPInputStream(inputStream()).readBytes()
    } else this


/**
 * Mark the current position, read the first ASCII line, reset, and return it.
 * Used to peek CONNECT without consuming the stream.
 */
fun BufferedInputStream.markAndPeekLine(): String {
    val markLimit = 4096                     // plenty for request line + headers
    mark(markLimit)
    val sb = StringBuilder()
    while (true) {
        val b = read()
        if (b == -1 || b.toChar() == '\n') break
        if (b.toChar() != '\r') sb.append(b.toChar())
    }
    reset()
    return sb.toString()
}

/** Read a single CRLF-terminated ASCII line (strips CR/LF). */
fun BufferedInputStream.readLineAscii(): String {
    val sb = StringBuilder()
    while (true) {
        val ch = read()
        if (ch == -1 || ch.toChar() == '\n') break
        if (ch.toChar() != '\r') sb.append(ch.toChar())
    }
    return sb.toString()
}
