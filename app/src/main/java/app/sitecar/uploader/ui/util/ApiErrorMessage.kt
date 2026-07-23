package app.sitecar.uploader.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.sitecar.uploader.R
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
    val forbidden: String,
    val notFound: String,
    val server: String,
    val noConnection: String,
    val timeout: String,
    val unknown: String,
)

/** Löst die [ApiErrorMessages]-Texte im Composable-Kontext auf (siehe Klassendoc). */
@Composable
fun rememberApiErrorMessages(): ApiErrorMessages = ApiErrorMessages(
    unauthorized = stringResource(R.string.error_unauthorized),
    forbidden = stringResource(R.string.error_forbidden),
    notFound = stringResource(R.string.error_not_found),
    server = stringResource(R.string.error_server),
    noConnection = stringResource(R.string.error_no_connection),
    timeout = stringResource(R.string.error_timeout),
    unknown = stringResource(R.string.error_unknown),
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
        // 401 = Key selbst ungültig/fehlt; 403 = Key gültig, aber ohne die nötige
        // Berechtigung (Scope) für diese Aktion — beides unter "API-Key prüfen" zu
        // fassen ist irreführend, wenn der Key für andere Aktionen längst funktioniert.
        apiException.status.value == 401 -> messages.unauthorized
        apiException.status.value == 403 -> messages.forbidden
        apiException.status.value == 404 -> messages.notFound
        apiException.status.value in 500..599 -> messages.server
        else -> apiException.message ?: messages.unknown
    }
}
