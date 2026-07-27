package app.dreamkorea.smartclass.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * StudyAlarmReceiver — fires when a study alarm goes off.
 * Shows a notification reminding the student to study Korean.
 */
class StudyAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra("label") ?: "Study Korean"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel (required for Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "study_alarms",
                "Study Alarms",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Daily study reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Build and show the notification
        val notification = NotificationCompat.Builder(context, "study_alarms")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("DreamKorea — Time to Study!")
            .setContentText(label)
            .setStyle(NotificationCompat.BigTextStyle().bigText("It's time for your Korean study session. $label — open the app and practice!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
