package com.rohitp.readerproxy.ui.onboarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@Composable
fun OnboardingNav(
    onFinished: () -> Unit,
) {
    val pager = rememberPagerState(0) { 3 }
    val scope = rememberCoroutineScope()

    fun next() = scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }

    HorizontalPager(
        state = pager,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> IntroPage(onNext = ::next)
            1 -> NotificationPage(onNext = ::next)
            2 -> CertificatePage(onDone = onFinished)
        }
    }
}
