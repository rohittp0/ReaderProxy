package com.rohitp.readerproxy.ui.onboarding

import androidx.compose.runtime.Composable
import com.rohitp.readerproxy.R
import com.rohitp.readerproxy.ui.PageScaffold

@Composable
internal fun IntroPage(onNext: () -> Unit) {
    PageScaffold(
        title = R.string.welcome_text,
        body = R.string.onboarding_intro_text,
        buttonText = R.string.next_button_text,
        buttonEnabled = true,
        onClick = onNext
    )
}
