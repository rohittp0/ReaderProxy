package com.rohitp.readerproxy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.rohitp.readerproxy.ui.HomeScreen
import com.rohitp.readerproxy.ui.onboarding.OnboardingNav
import com.rohitp.readerproxy.ui.theme.ReaderProxyTheme


class MainActivity : ComponentActivity() {
    private val sharedPrefs by lazy {
        getSharedPreferences("reader_proxy_prefs", MODE_PRIVATE)
    }

    private var onboardingDone by mutableStateOf(sharedPrefs.getBoolean("onboarding_done", false))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ReaderProxyTheme {
                MaterialTheme {
                    if (onboardingDone)
                        HomeScreen()
                    else
                        OnboardingNav(onFinished = ::onboardingFinished)
                }
            }
        }
    }

    private fun onboardingFinished() {
        onboardingDone = true
        sharedPrefs.edit { putBoolean("onboarding_done", true) }
    }
}
