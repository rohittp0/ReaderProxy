package com.rohitp.readerproxy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.rohitp.readerproxy.ui.HomeScreen
import com.rohitp.readerproxy.ui.onboarding.OnboardingNav
import com.rohitp.readerproxy.ui.theme.ReaderProxyTheme


class MainActivity : ComponentActivity() {
    private val sharedPrefs by lazy {
        getSharedPreferences("reader_proxy_prefs", MODE_PRIVATE)
    }

    private var hasNotifyPerm by mutableStateOf(
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
    )

    private var onboardingDone by mutableStateOf(sharedPrefs.getBoolean("onboarding_done", false))

    /* ── activity-result launchers ───────────────────────────────────── */
    private val notifyPermLauncher = registerForActivityResult(RequestPermission()) {
        hasNotifyPerm = it
    }

    /* ─────────────────────────────────────────────────────────────────── */
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

    /* ── helpers ─────────────────────────────────────────────────────── */
    private fun onboardingFinished() {
        onboardingDone = true
        sharedPrefs.edit { putBoolean("onboarding_done", true) }
    }


    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifyPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
