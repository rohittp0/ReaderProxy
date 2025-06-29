package com.rohitp.readerproxy.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.rohitp.readerproxy.ui.PageScaffold

@Composable
internal fun NotificationPage(
    onNext: () -> Unit
) {
    val context = LocalContext.current

    var hasPermission by remember {
        val notNeeded = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        mutableStateOf(
            notNeeded || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val notifyPermLauncher =
        rememberLauncherForActivityResult(RequestPermission()) { hasPermission = it }

    val askNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifyPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    PageScaffold(
        title = "Allow notifications",
        body = "We show a persistent notification while the VPN is active. " +
                "Android 13+ requires explicit permission.",
        buttonText = if (hasPermission) "Next" else "Grant Permission",
        buttonEnabled = true,
        onClick = if (hasPermission) onNext else askNotificationPermission
    )
}
