package app.sitecar.uploader.ui.util

import app.sitecar.uploader.data.ApiException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Vorab aufgelöste, lokalisierte Texte für [friendlyErrorMessage] — müssen aus
 * einem @Composable via stringResource() befüllt werden, da die Auswertung
 * selbst meist in einer Coroutine (onFailure-Lambda) läuft, nicht in
 * Composable-Kontext.
 */
data class ApiErrorMessages(
    val unauthorized: String,
    val notFound: String,
    val server: String,
    val noConnection: String,
    val timeout: String,
    val unknown: String,
)

/** Übersetzt technische Exceptions in eine für Nutzer verständliche Meldung. */
fun friendlyErrorMessage(throwable: Throwable, messages: ApiErrorMessages): String {
    val apiException = throwable as? ApiException
    return when {
        apiException == null -> when (throwable) {
            is UnknownHostException, is ConnectException -> messages.noConnection
            is SocketTimeoutException -> messages.timeout
            else -> throwable.message ?: messages.unknown
        }
        apiException.status.value == 401 || apiException.status.value == 403 -> messages.unauthorized
        apiException.status.value == 404 -> messages.notFound
        apiException.status.value in 500..599 -> messages.server
        else -> apiException.message ?: messages.unknown
    }
}
