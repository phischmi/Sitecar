package app.sitecar.uploader.data.reminders

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** Plant eine lokale Erinnerung für eine im Dokument erkannte Frist. */
object ReminderScheduler {

    fun schedule(context: Context, date: LocalDate, label: String, documentName: String) {
        val targetMillis = date.atTime(LocalTime.of(9, 0))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val delayMillis = (targetMillis - System.currentTimeMillis()).coerceAtLeast(0L)

        val data = Data.Builder()
            .putString(ReminderWorker.KEY_LABEL, label)
            .putString(ReminderWorker.KEY_DOCUMENT_NAME, documentName)
            .build()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}
