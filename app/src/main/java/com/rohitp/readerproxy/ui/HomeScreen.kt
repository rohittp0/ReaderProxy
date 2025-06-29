package com.rohitp.readerproxy.ui

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.rohitp.readerproxy.MyVpnService

@Composable
fun HomeScreen() {
    val ctx = LocalContext.current

    /* observe running state from the service */
    val isRunning by MyVpnService.isRunning.collectAsState(initial = false)
    var permissionDenied by remember { mutableStateOf(false) }

    val serviceIntent = remember { Intent(ctx, MyVpnService::class.java) }

    /* launcher for VPN permission */
    val vpnPermLauncher = rememberLauncherForActivityResult(StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            ContextCompat.startForegroundService(ctx, serviceIntent)
        } else {
            permissionDenied = true
        }
    }

    Scaffold { padd ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padd).wrapContentSize(Alignment.Center)
        ) {
            Text(
                if (permissionDenied)
                    "VPN permission denied. Please enable it in settings."
                else
                    "Welcome to Reader Proxy!"
            )

            /* button appearance depends on running state */
            val btnText = if (isRunning) "Stop Reader Mode" else "Start Reader Mode"
            val btnColors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )

            Button(
                modifier = Modifier.padding(16.dp),
                onClick = {
                    if (isRunning) {
                        ctx.startService(serviceIntent.apply {
                            action = MyVpnService.ACTION_STOP
                        })
                    } else {
                        val prep = VpnService.prepare(ctx)          // maybe ask
                        if (prep != null) vpnPermLauncher.launch(prep)
                        else ContextCompat.startForegroundService(ctx, serviceIntent)
                    }
                },
                colors = btnColors
            ) { Text(btnText) }
        }
    }
}
