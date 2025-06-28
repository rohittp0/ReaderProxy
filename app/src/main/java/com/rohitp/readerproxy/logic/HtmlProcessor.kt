package com.rohitp.readerproxy.logic

class HtmlProcessor {
    fun process(body: String, host: String): String {
        if(Constants.IGNORED_HOSTS.contains(host)) {
            return body // do not process
        }

        return body.replace("a", "b")
    }
}
