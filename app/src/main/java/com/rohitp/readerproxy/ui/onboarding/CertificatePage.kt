package com.rohitp.readerproxy.ui.onboarding

import android.app.Activity
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream

@Composable
internal fun CertificatePage(onDone: () -> Unit) {
    val context = LocalContext.current
    val fileName = "reader_proxy_ca.pem"

    var requested by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    val caInstallLauncher = rememberLauncherForActivityResult(
        contract = StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onDone()
        } else {
            failed = true
        }
    }

    fun launchInstallIntent() {
        requested = true

        val dst = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            ), fileName
        )

        val pem = context.assets.open("ca_cert.pem").use { it.readBytes() }

        if (!dst.exists())
            FileOutputStream(dst).use { fos ->
                fos.write(pem)
            }

        val intent = android.security.KeyChain.createInstallIntent().apply {
            putExtra(android.security.KeyChain.EXTRA_CERTIFICATE, pem)
            putExtra(android.security.KeyChain.EXTRA_NAME, "ReaderProxy CA")
        }

        caInstallLauncher.launch(intent)
    }

    fun openSettings() {
        requested = false
        failed = true

        val intent = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            .let {
                android.content.Intent(it)
                    .apply { data = Uri.fromParts("package", context.packageName, null) }
            }
        context.startActivity(intent)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Install local CA", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "To decrypt HTTPS pages Reader Proxy installs its own CA certificate, " +
                    "if skipped only pages that use HTTP will work.",
            style = MaterialTheme.typography.bodyLarge
        )

        if (failed) {
            Spacer(Modifier.height(24.dp))

            Text(
                "Failed to install CA. Please try to install manually from Downloads." +
                        "The CA file is named $fileName.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Spacer(Modifier.height(32.dp))
        Row {
            OutlinedButton(onClick = onDone) { Text("Skip") }
            Spacer(Modifier.width(16.dp))
            when (true) {
                (!requested && !failed) -> Button(onClick = ::launchInstallIntent) { Text("Install") }
                (requested && failed) -> Button(onClick = ::openSettings) { Text("Open Settings") }
                else -> Button(onClick = onDone) { Text("Done") }
            }
        }
    }
}

@Preview
@Composable
private fun CertificatePagePreview() {
    CertificatePage(onDone = {})
}
