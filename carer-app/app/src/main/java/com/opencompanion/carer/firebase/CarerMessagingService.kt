package com.opencompanion.carer.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.opencompanion.carer.R
import com.opencompanion.carer.ui.AlertDetailActivity
import com.opencompanion.carer.ui.MainActivity

class CarerMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_EMERGENCY = "emergency_alerts"
        const val CHANNEL_NORMAL = "normal_alerts"
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        when (message.data["type"]) {
            "emergency" -> showEmergencyAlert(message.data)
            "red_flag" -> showAlert(message.data, high = true)
            "daily_summary" -> showAlert(message.data, high = false)
            "check_in_missed" -> showAlert(message.data, high = false)
        }
    }

    private fun showEmergencyAlert(data: Map<String, String>) {
        val intent = Intent(this, AlertDetailActivity::class.java).apply {
            putExtra("type", "emergency")
            putExtra("timestamp", System.currentTimeMillis())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val fullScreenIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_EMERGENCY)
            .setSmallIcon(R.drawable.ic_alert)
            .setContentTitle(getString(R.string.alert_emergency))
            .setContentText(getString(R.string.alert_emergency_body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(fullScreenIntent, true)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showAlert(data: Map<String, String>, high: Boolean) {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_NORMAL)
            .setSmallIcon(R.drawable.ic_alert)
            .setContentTitle(data["title"] ?: getString(R.string.app_name))
            .setContentText(data["body"] ?: "")
            .setPriority(if (high) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_EMERGENCY, "Emergency Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Immediate alerts when elder triggers emergency phrase"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_NORMAL, "Activity Alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Daily summaries and activity notifications"
            }
        )
    }
}
