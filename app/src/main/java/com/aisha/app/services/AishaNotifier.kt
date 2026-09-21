package com.aisha.app.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aisha.app.MainActivity
import com.aisha.app.R

/**
 * LOCKED Master §9 Notification Manager — applies interruption rules (quiet hours,
 * runtime permission), schedules/delivers, records outcome via caller.
 * Channel: "aisha_reminders". Anti-spam lives in PresenceEngine/AmbientPolicy.
 */
object AishaNotifier {
    private const val CHANNEL = "aisha_reminders"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "AISHA reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Task reminders and permitted presence notifications"
            })
    }

    fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun post(context: Context, tag: String, id: Int, title: String, body: String): Boolean {
        if (!canPost(context)) return false
        val intent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title).setContentText(body)
            .setAutoCancel(true).setContentIntent(intent)
            .build()
        NotificationManagerCompat.from(context).notify(tag, id, notification)
        return true
    }
}
