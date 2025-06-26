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
import kotlin.concurrent.thread


class MyVpnService : VpnService() {

    companion object {
        private const val VPN_ADDRESS = "10.10.0.2"
        private const val VPN_PREFIX = 32           // /32 single host
        private const val PROXY_PORT = 8888
        private const val NOTIFICATION_CHANNEL_ID = "reader_mode_vpn_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "Reader Mode VPN"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var proxyThread: Thread? = null

    override fun onCreate() {
        super.onCreate()
        // Start the local proxy inside a background thread
        proxyThread = thread(name = "local-http-proxy") {
            ProxyServer(this@MyVpnService, PROXY_PORT).startBlocking()
        }
    }

    override fun onStartCommand(i: Intent?, f: Int, s: Int): Int {
        if(i?.action == "STOP_VPN") {
            // Stop the VPN service if requested
            vpnInterface?.close()
            vpnInterface = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        if (vpnInterface != null) return START_STICKY

        establishVpn()
        startForeground(1, updateNotification(R.string.vpn_started))

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

    private fun updateNotification(message: Int): Notification {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        val stopIcon = Icon.createWithResource(this, R.drawable.ic_launcher_foreground)

        val stopIntentAction = Notification.Action.Builder(
            stopIcon,
            getString(R.string.stop_vpn),
            PendingIntent.getService(
                this, 0, Intent(this, MyVpnService::class.java).apply {
                    action = "STOP_VPN"
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(message))
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use a suitable icon
            .setOngoing(true)
            .setActions(stopIntentAction.build())
            .build()

        return notification
    }

    override fun onDestroy() {
        proxyThread?.interrupt()
        vpnInterface?.close()
        super.onDestroy()
    }
}
