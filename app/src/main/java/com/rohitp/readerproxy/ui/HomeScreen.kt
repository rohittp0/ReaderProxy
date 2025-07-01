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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.rohitp.readerproxy.MyVpnService
import com.rohitp.readerproxy.MyVpnService.Companion.ACTION_STOP
import com.rohitp.readerproxy.R

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
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "menu")
                    }
                    DropdownMenu(showMenu, { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.install_certificate_menu_item)) },
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
                    stringResource(R.string.vpn_permission_denied)
                else
                    stringResource(R.string.welcome_text),
            )

            /* button appearance depends on running state */
            val btnText = if (isRunning) R.string.stop_button_text else R.string.start_button_text
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
                        val intent = Intent(ctx, MyVpnService::class.java).apply {
                            action = ACTION_STOP
                        }
                        ContextCompat.startForegroundService(ctx, intent)
                    } else {
                        val prep = VpnService.prepare(ctx)          // maybe ask
                        if (prep != null) vpnPermLauncher.launch(prep)
                        else ContextCompat.startForegroundService(ctx, serviceIntent)
                    }
                },
                colors = btnColors
            ) { Text(stringResource(btnText)) }
        }
    }
}
