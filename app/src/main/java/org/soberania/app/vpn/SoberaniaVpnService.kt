package org.soberania.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import org.soberania.app.MainActivity
import org.soberania.app.R

class SoberaniaVpnService : VpnService() {

    private var tunInterface: ParcelFileDescriptor? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopTunnel()
            ACTION_START -> startLabTunnel()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        closeTun()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopTunnel()
        super.onRevoke()
    }

    private fun startLabTunnel() {
        if (isRunning) return

        startForeground(NOTIFICATION_ID, buildNotification())

        /*
         * M0 deliberately DOES NOT install a default route.
         *
         * A full 0.0.0.0/0 or ::/0 route without a real packet-forwarding
         * transport would black-hole the user's network while looking like a
         * working privacy product.
         *
         * We create a TUN interface and route only documentation/test ranges.
         * This proves the Android VpnService lifecycle without pretending that
         * traffic is protected before the transport engine exists.
         */
        tunInterface = Builder()
            .setSession(getString(R.string.app_name))
            .setMtu(1500)
            .addAddress("10.77.0.1", 32)
            .addAddress("fd00:736f:6265::1", 128)
            .addRoute("192.0.2.0", 24)
            .addRoute("2001:db8::", 32)
            .establish()

        isRunning = tunInterface != null

        if (!isRunning) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopTunnel() {
        closeTun()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        stopSelf()
    }

    private fun closeTun() {
        isRunning = false
        runCatching { tunInterface?.close() }
        tunInterface = null
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_shield)
                .setContentTitle("Soberania — M0")
                .setContentText("Túnel de laboratório ativo; tráfego real ainda não está protegido.")
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_shield)
                .setContentTitle("Soberania — M0")
                .setContentText("Túnel de laboratório ativo; tráfego real ainda não está protegido.")
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .build()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Estado da proteção",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Mostra quando o núcleo de rede do Soberania está ativo."
        }

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "org.soberania.app.action.START"
        const val ACTION_STOP = "org.soberania.app.action.STOP"

        private const val CHANNEL_ID = "soberania_protection"
        private const val NOTIFICATION_ID = 1701

        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
