package com.rohitp.readerproxy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.ProxyInfo
import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.rohitp.readerproxy.proxy.ProxyServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.concurrent.thread


class MyVpnService : VpnService() {

    companion object {
        private const val VPN_ADDRESS = "10.10.0.2"
        private const val VPN_PREFIX = 32           // /32 single host
        private const val PROXY_PORT = 8888
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "reader_mode_vpn_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "Reader Mode VPN"
        internal const val ACTION_STOP = "STOP_VPN"

        val isRunning = MutableStateFlow(false)
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var proxyServer: ProxyServer? = null
    private var proxyThread: Thread? = null

    override fun onCreate() {
        super.onCreate()
        isRunning.value = true

        proxyServer = ProxyServer(this, PROXY_PORT)
        proxyThread = thread(name = "local-http-proxy") {
            proxyServer?.startBlocking()
        }
    }

    override fun onStartCommand(intent: Intent?, f: Int, s: Int): Int {
        if (intent?.action == ACTION_STOP) {
            tearDown()
            return START_NOT_STICKY
        }

        if (vpnInterface != null) return START_STICKY

        createNotificationChannel()
        startForeground(1, createNotification(R.string.vpn_starting))
        establishVpn()

        return START_STICKY
    }

    private fun establishVpn() {
        val proxyInfo = ProxyInfo.buildDirectProxy("127.0.0.1", PROXY_PORT)

        val builder = Builder()
            .setSession("ReaderModeVPN")
            .addAddress(VPN_ADDRESS, VPN_PREFIX)
            .addAllowedApplication("com.android.chrome")
            .allowBypass()
            .addDnsServer("8.8.8.8")
            .addRoute("0.0.0.0", 0)          // capture all IPv4
            .setHttpProxy(proxyInfo)

        vpnInterface = builder.establish()
        if (vpnInterface != null) {
            updateNotification(R.string.vpn_started)
        } else {
            updateNotification(R.string.vpn_failed)
        }
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun createStopIntent(): Notification.Action {
        val stopIcon = Icon.createWithResource(this, R.drawable.ic_launcher_foreground)

        val stopIntentAction = Notification.Action.Builder(
            stopIcon,
            getString(R.string.stop_vpn),
            PendingIntent.getService(
                this, 0, Intent(this, MyVpnService::class.java).apply {
                    action = ACTION_STOP
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        return stopIntentAction.build()
    }

    private fun createNotification(message: Int): Notification {
        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(message))
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use a suitable icon
            .setOngoing(true)
            .setActions(createStopIntent())
            .build()

        return notification
    }

    private fun updateNotification(message: Int) {
        val notification = createNotification(message)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun tearDown() {
        proxyServer?.stop()                       // ① close ServerSocket cleanly
        proxyThread?.interrupt()
        vpnInterface?.close()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        isRunning.value = false
    }

    override fun onDestroy() {
        tearDown()
        super.onDestroy()
    }
}
