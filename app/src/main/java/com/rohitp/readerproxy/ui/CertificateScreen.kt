package com.rohitp.readerproxy.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.rohitp.readerproxy.ui.onboarding.CertificatePage

@Composable
fun CertificateScreen(nav: NavController) {
    CertificatePage {
        nav.navigate(Screens.Home.route) {
            popUpTo(Screens.Home.route) { inclusive = true }
        }
    }
}