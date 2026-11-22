package ca.machineh3d.lotgddcc.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ca.machineh3d.lotgddcc.MainActivity
import ca.machineh3d.lotgddcc.R
import ca.machineh3d.lotgddcc.data.DccApiClient
import ca.machineh3d.lotgddcc.data.ServerPreferences
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object MailNotificationScheduler {
    const val CHANNEL_ID = "dcc_mail_alerts"
    const val EXTRA_OPEN_MAIL = "extra_open_mail"
    private const val WORK_NAME = "dcc_mail_poll_worker"
    private const val NOTIFICATION_ID = 4202

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Syndicate Comms (In-Game Mail)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies you when a new in-game mail message arrives."
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun startPeriodicPolling(context: Context) {
        ensureChannel(context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<MailCheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun postMailNotification(context: Context, unreadCount: Int) {
        if (unreadCount <= 0) return
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_MAIL, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val msgText = if (unreadCount == 1) {
            "You have 1 new unread Syndicate Comms message. Tap to open your inbox."
        } else {
            "You have $unreadCount unread Syndicate Comms messages. Tap to open your inbox."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_dcc)
            .setContentTitle("New Syndicate Comms — Dungeon Crawler World")
            .setContentText(msgText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}

class MailCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = ServerPreferences(applicationContext)
        val snap = prefs.prefsFlow.first()
        if (!snap.mailNotificationsEnabled || snap.lastServerUrl.isBlank()) {
            return Result.success()
        }

        return try {
            val api = DccApiClient(applicationContext)
            val resp = api.fetchPage(snap.lastServerUrl, "village.php")
            if (resp.player?.loggedIn == true) {
                val unread = resp.mail.unread
                if (unread > snap.lastNotifiedUnreadMail) {
                    MailNotificationScheduler.postMailNotification(applicationContext, unread)
                }
                prefs.saveLastNotifiedUnreadMail(unread)
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
