package app.sitecar.uploader.data.reminders

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import app.sitecar.uploader.R
import kotlin.random.Random

/**
 * Zeigt eine lokale Benachrichtigung für eine im Dokument erkannte Frist
 * (z. B. Kündigungsfrist, Garantie-Ablauf). Läuft komplett lokal über
 * WorkManager, kein Server-/Netzwerkzugriff.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val label = inputData.getString(KEY_LABEL) ?: applicationContext.getString(R.string.reminder_default_title)
        val documentName = inputData.getString(KEY_DOCUMENT_NAME).orEmpty()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            // Berechtigung wurde nach dem Planen widerrufen/nie erteilt – nichts zu tun.
            return Result.success()
        }

        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(applicationContext.getString(R.string.reminder_channel_name))
            .build()
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shortcut_scan)
            .setContentTitle(label)
            .setContentText(documentName)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching { notificationManager.notify(Random.nextInt(), notification) }

        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "document_reminders"
        const val KEY_LABEL = "label"
        const val KEY_DOCUMENT_NAME = "document_name"
    }
}
