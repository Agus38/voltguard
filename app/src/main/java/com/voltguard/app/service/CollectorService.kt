package com.voltguard.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.voltguard.app.MainActivity
import com.voltguard.app.R
import com.voltguard.app.VoltGuardApp
import com.voltguard.app.data.AlertEvent
import com.voltguard.app.data.AlertLevel
import com.voltguard.app.data.PowerFormatters
import com.voltguard.app.data.PowerSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps sampling battery/charger state and surfaces
 * alerts as notifications. Monitoring continues after the UI is closed; a
 * persistent notification reflects the current status.
 */
class CollectorService : Service() {

    private lateinit var scope: CoroutineScope
    private lateinit var repo: com.voltguard.app.data.PowerRepository

    override fun onCreate() {
        super.onCreate()
        val app = application as VoltGuardApp
        repo = app.repository
        createChannel()
        startForeground(NOTIF_ID, baseNotification())
        repo.start()

        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope.launch {
            repo.snapshot.collectLatest { snap ->
                try {
                    getSystemService(NotificationManager::class.java)
                        ?.notify(NOTIF_ID, statusNotification(snap))
                } catch (_: Throwable) {}
            }
        }
        scope.launch {
            repo.alerts.collectLatest { alert ->
                try {
                    if (alert.level == AlertLevel.ALERT && repo.settings.value.alertEnabled) {
                        notifyAlert(alert)
                    }
                    if (repo.settings.value.soundEnabled) vibrate()
                } catch (_: Throwable) {}
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        repo.stop()
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notif_channel_desc)
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERT_ID, "Alert tegangan", NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Peringatan tegangan & suhu"
                enableVibration(true)
            }
        )
    }

    private fun openIntent(): PendingIntent {
        val i = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this, 0, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun baseNotification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_bolt)
        .setContentTitle(getString(R.string.app_name))
        .setContentText(getString(R.string.notif_ongoing))
        .setOngoing(true)
        .setContentIntent(openIntent())
        .build()

    private fun statusNotification(s: PowerSnapshot): Notification {
        val title = when {
            s.isCharging -> "Mengisi • ${s.statusText}"
            s.isUnpluggedFull -> "Baterai penuh"
            s.isDischarging -> s.statusText
            else -> s.statusText
        }
        val v = s.vinVoltage?.takeIf { it > 0f } ?: s.voltage
        val body = "${PowerFormatters.percent(s.level)}  •  ${PowerFormatters.temp(s.temperature)}\n" +
                "V: ${PowerFormatters.volt(v)}   P: ${PowerFormatters.power(s.power)}"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_bolt)
            .setContentTitle(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setOngoing(true)
            .setContentIntent(openIntent())
            .build()
    }

    private fun notifyAlert(a: AlertEvent) {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.notify(ALERT_NOTIF_ID, NotificationCompat.Builder(this, CHANNEL_ALERT_ID)
            .setSmallIcon(R.drawable.ic_stat_bolt)
            .setContentTitle(a.title)
            .setContentText(a.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(a.message))
            .setAutoCancel(true)
            .setContentIntent(openIntent())
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build())
    }

    private fun vibrate() {
        val v = getSystemService(VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(300)
        }
    }

    companion object {
        const val CHANNEL_ID = "voltguard_monitor"
        const val CHANNEL_ALERT_ID = "voltguard_alerts"
        const val NOTIF_ID = 1001
        const val ALERT_NOTIF_ID = 1002
    }
}
