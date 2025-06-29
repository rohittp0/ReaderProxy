package com.rohitp.readerproxy.ui.onboarding

import androidx.compose.runtime.Composable
import com.rohitp.readerproxy.ui.PageScaffold

@Composable
internal fun IntroPage(onNext: () -> Unit) {
    PageScaffold(
        title = "Welcome to Reader Proxy",
        body = "This app turns every web page into a clutter-free reader view. " +
                "It runs a local VPN & proxy to rewrite HTML on the fly.",
        buttonText = "Next",
        buttonEnabled = true,
        onClick = onNext
    )
}
