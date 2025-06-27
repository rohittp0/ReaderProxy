package com.rohitp.readerproxy

import java.io.BufferedInputStream

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
