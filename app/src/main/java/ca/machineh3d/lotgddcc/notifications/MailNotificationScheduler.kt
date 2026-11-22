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
    const val CHANNEL_MAIL_SOUND = "dcc_mail_alerts_sound"
    const val CHANNEL_MAIL_SILENT = "dcc_mail_alerts_silent"
    const val CHANNEL_PVP_SOUND = "dcc_pvp_alerts_sound"
    const val CHANNEL_PVP_SILENT = "dcc_pvp_alerts_silent"
    const val EXTRA_OPEN_MAIL = "extra_open_mail"
    private const val WORK_NAME = "dcc_mail_poll_worker"
    private const val MAIL_NOTIFICATION_ID = 4202
    private const val PVP_NOTIFICATION_ID = 4203

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MAIL_SOUND,
                    "Syndicate Comms (Sound)",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Alerts with sound when new in-game mail arrives."
                }
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MAIL_SILENT,
                    "Syndicate Comms (Silent)",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Silently alerts when new in-game mail arrives."
                    setSound(null, null)
                }
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_PVP_SOUND,
                    "PvP Attack Alerts (Sound)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Urgent alert with sound when another Crawler attacks you."
                }
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_PVP_SILENT,
                    "PvP Attack Alerts (Silent)",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Silent alert when another Crawler attacks you."
                    setSound(null, null)
                }
            )
        }
    }

    fun startPeriodicPolling(context: Context) {
        ensureChannels(context)
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

    fun postMailNotification(
        context: Context,
        unreadCount: Int,
        soundEnabled: Boolean = true,
    ) {
        if (unreadCount <= 0) return
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return

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

        val channelId = if (soundEnabled) CHANNEL_MAIL_SOUND else CHANNEL_MAIL_SILENT
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_dcc)
            .setContentTitle("New Syndicate Comms — Dungeon Crawler World")
            .setContentText(msgText)
            .setPriority(if (soundEnabled) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_LOW)
            .setSilent(!soundEnabled)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(MAIL_NOTIFICATION_ID, notification)
    }

    fun postPvpAttackNotification(
        context: Context,
        detail: String,
        soundEnabled: Boolean = true,
    ) {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_MAIL, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (soundEnabled) CHANNEL_PVP_SOUND else CHANNEL_PVP_SILENT
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_dcc)
            .setContentTitle("PvP Attack Alert — Dungeon Crawler World")
            .setContentText(detail)
            .setPriority(if (soundEnabled) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .setSilent(!soundEnabled)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(PVP_NOTIFICATION_ID, notification)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}

class MailCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = ServerPreferences(applicationContext)
        val snap = prefs.prefsFlow.first()
        val anyEnabled = snap.mailNotificationsEnabled || snap.pvpNotificationsEnabled
        if (!anyEnabled || snap.lastServerUrl.isBlank()) {
            return Result.success()
        }

        return try {
            val api = DccApiClient(applicationContext)
            val resp = api.fetchPage(snap.lastServerUrl, "mail.php")
            val player = resp.player
            if (player?.loggedIn == true) {
                val unread = resp.mail.unread
                val isAlive = player.alive
                var pvpAlertSent = false

                // 1. Detect offline death (classic PvP kill while resting at the Inn/Safe Room)
                if (snap.pvpNotificationsEnabled && snap.lastKnownAlive && !isAlive) {
                    MailNotificationScheduler.postPvpAttackNotification(
                        context = applicationContext,
                        detail = "Your Crawler was slain while offline! Tap to inspect Syndicate Comms.",
                        soundEnabled = snap.notificationSoundsEnabled,
                    )
                    pvpAlertSent = true
                }

                // 2. If new unread mail arrived, check if it's a PvP attack report
                if (unread > snap.lastNotifiedUnreadMail) {
                    var foundPvpMail = false
                    if (snap.pvpNotificationsEnabled && !pvpAlertSent) {
                        runCatching {
                            val mailPage = api.fetchPage(snap.lastServerUrl, resp.mail.url.ifBlank { "mail.php" })
                            val mailText = buildString {
                                mailPage.paragraphs.forEach { line ->
                                    append(line.joinToString("") { it.text }).append(' ')
                                }
                                mailPage.tables.forEach { t ->
                                    t.rows.forEach { r -> append(r.cells.joinToString(" ")).append(' ') }
                                }
                            }
                            foundPvpMail = listOf(
                                "slain in the",
                                "attacked you",
                                "killed you",
                                "pvp",
                                "slain you",
                                "ambushed you",
                            ).any { mailText.contains(it, ignoreCase = true) }
                        }
                    }

                    if (foundPvpMail) {
                        MailNotificationScheduler.postPvpAttackNotification(
                            context = applicationContext,
                            detail = "Another Crawler attacked you! Tap to read the combat report.",
                            soundEnabled = snap.notificationSoundsEnabled,
                        )
                    } else if (snap.mailNotificationsEnabled) {
                        MailNotificationScheduler.postMailNotification(
                            context = applicationContext,
                            unreadCount = unread,
                            soundEnabled = snap.notificationSoundsEnabled,
                        )
                    }
                }

                prefs.saveLastNotifiedUnreadMail(unread)
                prefs.saveLastKnownAlive(isAlive)
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
