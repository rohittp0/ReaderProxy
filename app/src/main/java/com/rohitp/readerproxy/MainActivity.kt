package com.rohitp.readerproxy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.rohitp.readerproxy.ui.theme.ReaderProxyTheme

class MainActivity : ComponentActivity() {
    private lateinit var vpnPermissionLauncher: ActivityResultLauncher<Intent>
    private lateinit var caInstallRequest: ActivityResultLauncher<Intent>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vpnInit()
        initCaInstall()
        notificationInit()

        // 5. UI
        enableEdgeToEdge()
        setContent {
            ReaderProxyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center)
                    ) {
                        Column {
                            Button(onClick = {
                                checkAndStartVpn()
                            }) {
                                Text(text = "Start Reader Mode")
                            }
                            Button(onClick = {
                                promptInstallCa()
                            }) {
                                Text(text = "Install CA Certificate")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun promptInstallCa() {
        // 1. Load the PEM bytes
        val pemBytes = assets.open("ca_cert.pem").use { it.readBytes() }

        // 2. Build the install-intent
        val intent = android.security.KeyChain.createInstallIntent().apply {
            putExtra(android.security.KeyChain.EXTRA_CERTIFICATE, pemBytes)
            putExtra(android.security.KeyChain.EXTRA_NAME, "ReaderProxy CA")
        }

        caInstallRequest.launch(intent)
    }

    private fun initCaInstall() {
        caInstallRequest = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                // CA installed successfully
                Toast.makeText(
                    this, "CA installed successfully. Reader Mode Proxy can now run.",
                    Toast.LENGTH_LONG
                ).show()
                checkAndStartVpn()
            } else {
                // CA installation failed or was cancelled
                Toast.makeText(
                    this,
                    "CA installation failed. Reader Mode Proxy can't intercept HTTPS traffic.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun vpnInit() {
        vpnPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                startVpn()
            } else {
                Toast.makeText(
                    this, "VPN permission denied. Reader Mode Proxy cannot run.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun notificationInit() {
        // 2. Setup notification permission launcher (Android 13+)
        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            // You can show rationale or disable features if not granted
        }

        // 3. Request POST_NOTIFICATIONS on Android 13+
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        )
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun checkAndStartVpn() {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            vpnPermissionLauncher.launch(vpnIntent)
        } else {
            startVpn()
        }
    }

    private fun startVpn() {
        startService(Intent(this, MyVpnService::class.java))
        Toast.makeText(
            this, "Reader Mode Proxy started. Open Chrome to use it.",
            Toast.LENGTH_LONG
        ).show()
    }
}
