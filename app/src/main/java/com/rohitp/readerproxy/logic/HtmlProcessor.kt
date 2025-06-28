package com.rohitp.readerproxy.logic

import com.rohitp.readerproxy.proxy.Protocol
import net.dankito.readability4j.extended.Readability4JExtended
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Entities
import org.jsoup.safety.Safelist

val css = """
body{
     font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Helvetica,Arial,sans-serif;
     max-width:42em;
     margin:auto;
    padding:1em;
    line-height:1.6;
    font-size:1.05em;
    background:#1E1F22;
    color: #BCBEC4
}
 img,video{
    max-width:100%;
    height:auto
}
 h1{
    font-size:1.8em;
    margin-top:0
}
 h2,h3{
    margin-top:1.4em
}
 pre{
    white-space:pre-wrap;
    background:#f4f4f4;
    padding:.6em;
    border-radius:.4em
}

""".trimIndent()

class HtmlProcessor {

    /** Returns reader-mode HTML. On failure returns [body] unchanged. */
    fun process(body: String, protocol: Protocol, host: String, path: String): String {
        if (Constants.IGNORED_HOSTS.contains(host)) return body

        val baseUrl = "${protocol.value}://$host"
        val reader = if (path.startsWith("/")) "$baseUrl$path" else "$baseUrl/$path"

        return runCatching { readerMode(body, reader) }.getOrDefault(body)
    }

    /* ── internal helpers ─────────────────────────────────────────────── */

    private fun readerMode(html: String, uri: String): String {
        val reader = Readability4JExtended(uri, html)
        /* 1. Extract article */
        val article = reader.parse()
        val contentHtml = article.content      // may be <div>… or null
        val title = article.title ?: "Reader View"

        /* 2. Sanitize & tidy with Jsoup */
        val doc: Document = Jsoup.parse("<html><head></head><body>$contentHtml</body></html>")
        doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml)
        doc.select("script, noscript, style, iframe, nav, header, footer, aside").remove()
        Jsoup.clean(doc.html(), Safelist.relaxed().addTags("figure", "figcaption"))

        doc.head().append("<meta charset=\"utf-8\"><title>$title</title>")
        doc.head().append("<style>$css</style>")

        return doc.outerHtml()
    }
}
