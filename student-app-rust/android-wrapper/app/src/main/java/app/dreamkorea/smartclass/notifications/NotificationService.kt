package app.dreamkorea.smartclass.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.dreamkorea.smartclass.MainActivity
import app.dreamkorea.smartclass.R
import app.dreamkorea.smartclass.api.AppNotification
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * NotificationService — polls the server for new notifications every 60 seconds
 * and shows them as local Android notifications.
 *
 * No FCM/external service needed — works with simple polling.
 * Admin sends a notification via the admin panel → it's stored in DB →
 * all student apps pick it up on the next poll and show it locally.
 */
object NotificationService {
    private const val CHANNEL_ID = "dreamkorea_notifications"
    private const val CHANNEL_NAME = "DreamKorea Updates"
    private const val POLL_INTERVAL_MS = 60_000L // 60 seconds
    private const val PREFS_NAME = "dreamkorea_notif"
    private const val KEY_LAST_FETCH = "last_fetch_time"

    private var pollJob: Job? = null

    /** Create the notification channel (required for Android 8.0+). */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications from DreamKorea admin"
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /** Check if notification permission is granted (Android 13+). */
    fun hasPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Start polling for notifications. Called when the app starts (after login).
     * Runs in a background coroutine that survives across screen navigations.
     */
    fun startPolling(context: Context) {
        pollJob?.cancel()
        pollJob = CoroutineScope(Dispatchers.IO).launch {
            createChannel(context)
            // Poll immediately, then every 60 seconds
            while (true) {
                try {
                    if (AppState.isLoggedIn() && hasPermission(context)) {
                        fetchAndShow(context)
                    }
                } catch (_: Exception) {
                    // Network errors are expected — just retry next cycle
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    /** Stop polling (called on logout). */
    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private suspend fun fetchAndShow(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastFetch = prefs.getString(KEY_LAST_FETCH, null)
        val since = lastFetch ?: ""
        val response = AppState.api.getNotifications(if (since.isNotEmpty()) since else null)
        // Update last fetch time to now (ISO format)
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        prefs.edit().putString(KEY_LAST_FETCH, now).apply()

        // Show notifications for all new items
        for (notif in response.notifications) {
            showNotification(context, notif)
        }
    }

    private fun showNotification(context: Context, notif: AppNotification) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notif.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(notif.title)
            .setContentText(notif.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notif.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notif.id.hashCode(), builder.build())
        } catch (_: SecurityException) {
            // Permission was revoked — skip
        }
    }
}
