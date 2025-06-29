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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.navigation.NavController
import com.rohitp.readerproxy.MyVpnService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavController) {
    val ctx = LocalContext.current

    /* observe running state from the service */
    val isRunning by MyVpnService.isRunning.collectAsState(initial = false)
    var permissionDenied by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val serviceIntent = remember { Intent(ctx, MyVpnService::class.java) }

    /* launcher for VPN permission */
    val vpnPermLauncher = rememberLauncherForActivityResult(StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            ContextCompat.startForegroundService(ctx, serviceIntent)
        } else {
            permissionDenied = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reader Proxy") },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "menu")
                    }
                    DropdownMenu(showMenu, { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Install certificate") },
                            onClick = {
                                showMenu = false
                                nav.navigate(Screens.Certificate.route)
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).wrapContentSize(Alignment.Center)
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
