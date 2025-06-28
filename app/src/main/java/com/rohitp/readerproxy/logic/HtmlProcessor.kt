package com.rohitp.readerproxy.logic

import com.rohitp.readerproxy.proxy.Protocol
import java.util.Base64      // Java 8+, available on Android 26+
import net.dankito.readability4j.extended.Readability4JExtended
import org.jsoup.Jsoup
import org.jsoup.nodes.Entities
import org.jsoup.safety.Safelist

/** Dark reader-mode CSS. Tweak freely. */
private const val READER_CSS = """
body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Helvetica,Arial,sans-serif;
     max-width:42em;margin:auto;padding:1em;line-height:1.6;font-size:1.05em;
     background:#1E1F22;color:#BCBEC4}
img,video{max-width:100%;height:auto}
h1{font-size:1.8em;margin-top:0}
h2,h3{margin-top:1.4em}
pre{white-space:pre-wrap;background:#292A2D;padding:.6em;border-radius:.4em}
"""

private const val FAB_CSS = """
#readerFab{
  position:fixed;right:1.2rem;bottom:1.2rem;width:56px;height:56px;border:none;
  border-radius:50%;background:#03DAC6;color:#000;font-size:26px;cursor:pointer;
  box-shadow:0 3px 6px rgba(0,0,0,.3);z-index:16777271}
"""

class HtmlProcessor {

    /**
     * @param body     raw HTML from the server
     * @param protocol http / https
     * @param host     example.com
     * @param path     /foo/bar
     */
    fun process(body: String, protocol: Protocol, host: String, path: String): String {
        if (host in Constants.IGNORED_HOSTS) return body

        val url   = buildUrl(protocol, host, path)
        val readerHtml = buildReaderHtml(body, url)         // heavy work once
        val encoded    = Base64.getEncoder().encodeToString(readerHtml.toByteArray())

        return injectToggleFab(body, encoded)
    }

    /* ───────────────────── helpers ───────────────────── */

    private fun buildUrl(proto: Protocol, host: String, path: String): String =
        "${proto.value}://$host" + if (path.startsWith("/")) path else "/$path"

    /** Returns full, standalone reader-mode HTML (dark CSS already inlined). */
    private fun buildReaderHtml(original: String, url: String): String {
        val article = Readability4JExtended(url, original).parse()
        val clean   = Jsoup.parse("<html><head></head><body>${article.content}</body></html>")
        clean.outputSettings().escapeMode(Entities.EscapeMode.xhtml)
        // strip anything dangerous then re-add relaxed set
        Jsoup.clean(clean.html(), Safelist.relaxed().addTags("figure","figcaption"))

        val title = (article.title ?: "Reader View").replace("\"", "&quot;")
        clean.head().append("<meta charset='utf-8'><title>$title</title>")
        clean.head().append("<style>$READER_CSS</style>")
        return clean.outerHtml()
    }

    /** Injects FAB, CSS, and toggle script into the *original* page. */
    private fun injectToggleFab(originalHtml: String, readerBase64: String): String {
        val doc = Jsoup.parse(originalHtml)
        doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml)

        doc.head().append("<style>$FAB_CSS</style>")
        doc.body().append("""<button id="readerFab">📖</button>""")

        val script = """
            <script>
            (function(){
              const b64 = "$readerBase64";
              const readerHtml = atob(b64);
              let switched = false;
              document.getElementById('readerFab').addEventListener('click', () => {
                 if (switched) return;
                 switched = true;
                 document.open();
                 document.write(readerHtml);
                 document.close();
              });
            })();
            </script>
        """.trimIndent()

        doc.body().append(script)
        return doc.outerHtml()
    }
}
