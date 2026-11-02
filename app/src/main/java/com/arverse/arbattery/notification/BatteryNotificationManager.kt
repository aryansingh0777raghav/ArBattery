package com.arverse.arbattery.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arverse.arbattery.core.common.model.ChargingState
import com.arverse.arbattery.core.common.model.DeviceBattery

class BatteryNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_THRESHOLDS = "channel_arbattery_thresholds"
        const val CHANNEL_MONITORING = "channel_arbattery_monitoring"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Map of deviceId to highest alert level already notified (20 or 10)
    private val notifiedLevels = mutableMapOf<String, Int>()

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val thresholdChannel = NotificationChannel(
                CHANNEL_THRESHOLDS,
                "Battery Threshold Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent notifications when devices reach 20% or 10% battery"
                enableVibration(true)
                setShowBadge(true)
            }

            val monitoringChannel = NotificationChannel(
                CHANNEL_MONITORING,
                "Hub Monitoring Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background telemetry status for companion and BLE devices"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(thresholdChannel)
            notificationManager.createNotificationChannel(monitoringChannel)
        }
    }

    fun handleBatteryUpdate(device: DeviceBattery, lowThreshold: Int = 20, criticalThreshold: Int = 10) {
        val pct = device.batteryPercentage ?: return

        // If charging, clear alert state
        if (device.chargingState == ChargingState.CHARGING) {
            notifiedLevels.remove(device.deviceId)
            return
        }

        // Check Critical (<=10%)
        if (pct <= criticalThreshold) {
            val lastLevel = notifiedLevels[device.deviceId] ?: 100
            if (lastLevel > criticalThreshold) {
                sendCriticalAlert(device, pct)
                notifiedLevels[device.deviceId] = criticalThreshold
            }
            return
        }

        // Check Low (<=20%)
        if (pct <= lowThreshold) {
            val lastLevel = notifiedLevels[device.deviceId] ?: 100
            if (lastLevel > lowThreshold) {
                sendLowAlert(device, pct)
                notifiedLevels[device.deviceId] = lowThreshold
            }
            return
        }

        // If battery recovered above lowThreshold (e.g. charged and unplugged)
        if (pct > lowThreshold) {
            notifiedLevels.remove(device.deviceId)
        }
    }

    private fun sendLowAlert(device: DeviceBattery, pct: Int) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
        val pendingIntent = PendingIntent.getActivity(
            context,
            device.deviceId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_THRESHOLDS)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Low Battery: ${device.deviceName}")
            .setContentText("${device.deviceName} has reached $pct% battery.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(device.deviceId.hashCode(), notification)
        } catch (_: SecurityException) {
        }
    }

    private fun sendCriticalAlert(device: DeviceBattery, pct: Int) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
        val pendingIntent = PendingIntent.getActivity(
            context,
            device.deviceId.hashCode() + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_THRESHOLDS)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("🚨 CRITICAL BATTERY: ${device.deviceName}")
            .setContentText("${device.deviceName} is at $pct%! Connect charger immediately.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(device.deviceId.hashCode(), notification)
        } catch (_: SecurityException) {
        }
    }
}
