package app.sitecar.client.data

import android.app.Activity
import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Play In-App-Review: bittet einmalig um eine Bewertung, sobald die App ein paar
 * Tage genutzt und mehrfach erfolgreich hochgeladen wurde.
 *
 * Eigene Vorab-Abfragen ("Gefällt dir die App?") verbietet Google, deshalb wird
 * der Play-Dialog direkt gestartet. Ob er tatsächlich erscheint, entscheidet der
 * Play Store selbst (eigenes Kontingent) und verrät es der App nicht — daher gilt
 * die Anfrage danach in jedem Fall als erledigt.
 */
class ReviewPrompt(context: Context, private val store: SettingsStore) {

    private val manager = ReviewManagerFactory.create(context.applicationContext)

    private fun isDue(): Boolean {
        if (store.reviewPromptedAtMillis != 0L) return false
        if (store.uploadCount < MIN_UPLOADS) return false
        val firstLaunch = store.firstLaunchAtMillis
        if (firstLaunch == 0L) return false
        return System.currentTimeMillis() - firstLaunch >= MIN_USAGE_MILLIS
    }

    /**
     * Zeigt den Bewertungsdialog, falls fällig, und kehrt erst zurück, wenn er
     * wieder geschlossen ist. Fehler werden geschluckt: ohne Play Store (Debug-
     * Build, Sideload) schlägt schon das Anfordern des Flows fehl — dann bleibt
     * die Anfrage offen und wird beim nächsten Upload erneut versucht.
     */
    suspend fun requestIfDue(activity: Activity) {
        if (!isDue()) return
        val reviewInfo = runCatching { manager.requestReviewFlow().awaitResult() }.getOrNull() ?: return
        store.reviewPromptedAtMillis = System.currentTimeMillis()
        runCatching { manager.launchReviewFlow(activity, reviewInfo).awaitResult() }
    }

    private suspend fun <T> Task<T>.awaitResult(): T? = suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            val error = task.exception
            if (error != null) cont.resumeWith(Result.failure(error)) else cont.resume(task.result)
        }
    }

    private companion object {
        const val MIN_UPLOADS = 3
        val MIN_USAGE_MILLIS = TimeUnit.DAYS.toMillis(3)
    }
}
