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
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ca.machineh3d.lotgddcc.MainActivity
import ca.machineh3d.lotgddcc.R
import java.util.concurrent.TimeUnit

object NewDayScheduler {
    const val CHANNEL_ID = "dcc_new_day_alerts"
    private const val WORK_NAME = "dcc_new_day_notification"
    const val KEY_REPEAT_SECONDS = "repeat_seconds"
    private const val NOTIFICATION_ID = 4201

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "New Game Day Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies you when a new in-game day begins and your Stamina resets."
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleNextNewDay(
        context: Context,
        delaySeconds: Int,
        secondsPerGameDay: Int,
    ) {
        if (delaySeconds <= 0) return
        ensureChannel(context)

        val safeDelay = delaySeconds.coerceAtLeast(15).toLong()
        val data = Data.Builder()
            .putInt(KEY_REPEAT_SECONDS, secondsPerGameDay.coerceAtLeast(0))
            .build()

        val request = OneTimeWorkRequestBuilder<NewDayNotificationWorker>()
            .setInitialDelay(safeDelay, TimeUnit.SECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun postNotification(context: Context) {
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
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_dcc)
            .setContentTitle("New Game Day — Dungeon Crawler World")
            .setContentText("Your Stamina has reset! Tap to enter the World Dungeon.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}

class NewDayNotificationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        NewDayScheduler.postNotification(applicationContext)

        val repeatSeconds = inputData.getInt(NewDayScheduler.KEY_REPEAT_SECONDS, 0)
        if (repeatSeconds >= 600) {
            NewDayScheduler.scheduleNextNewDay(
                context = applicationContext,
                delaySeconds = repeatSeconds,
                secondsPerGameDay = repeatSeconds,
            )
        }
        return Result.success()
    }
}
