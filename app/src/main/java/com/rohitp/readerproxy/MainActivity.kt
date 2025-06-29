package com.rohitp.readerproxy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rohitp.readerproxy.ui.CertificateScreen
import com.rohitp.readerproxy.ui.HomeScreen
import com.rohitp.readerproxy.ui.Screens
import com.rohitp.readerproxy.ui.onboarding.OnboardingNav
import com.rohitp.readerproxy.ui.theme.ReaderProxyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ReaderProxyTheme {
                MaterialTheme {
                    /* observe the flag */
                    val done by AppSettings.onboardingDone(this).collectAsState(initial = false)

                    if (done) App()
                    else OnboardingNav(onFinished = ::onboardingFinished)
                }
            }
        }
    }

    private fun onboardingFinished() {
        /* update DataStore asynchronously */
        lifecycleScope.launch {
            AppSettings.markOnboardingDone(this@MainActivity)
        }
    }
}

@Composable
fun App() {
    val navController = rememberNavController()

    NavHost(navController, startDestination = Screens.Home.route) {
        composable(Screens.Home.route) {
            HomeScreen(navController)
        }
        composable(Screens.Certificate.route) {
            CertificateScreen(navController)
        }
    }
}

