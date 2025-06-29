package com.rohitp.readerproxy.ui

import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startForegroundService
import com.rohitp.readerproxy.MyVpnService

@Composable
fun HomeScreen() {
    val context = LocalContext.current

    var permissionDenied by remember { mutableStateOf(false) }

    val vpnPermLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            startForegroundService(context, Intent(context, MyVpnService::class.java))
        }
        else {
            permissionDenied = true
        }
    }

    val startVpn = {
        val intent = VpnService.prepare(context)
        if (intent != null)
            vpnPermLauncher.launch(intent)
        else
            startForegroundService(context, Intent(context, MyVpnService::class.java))
    }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).wrapContentSize(Alignment.Center)
        ) {
            if (permissionDenied) {
                Text("VPN permission denied. Please enable it in settings.")
            } else {
                Text("Welcome to Reader Proxy!")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = startVpn) { Text("Start Reader Mode VPN") }
        }
    }
}
