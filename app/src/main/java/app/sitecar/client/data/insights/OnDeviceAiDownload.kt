package app.sitecar.client.data.insights

import app.sitecar.client.data.SettingsStore
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Steuert den einmaligen, geräteweiten Download des Gemini-Nano-Modells für
 * [GenAiInsightsEngine] und macht dessen Fortschritt über [state] sichtbar.
 *
 * Läuft bewusst in einem app-weiten Scope statt im Scope des Einstellungs-Screens:
 * der Download dauert je nach Gerät und Verbindung Minuten, und wenn der Nutzer
 * die Einstellungen währenddessen verlässt, darf weder der Download abgebrochen
 * noch die anschließende Aktivierung verschluckt werden. Deshalb setzt dieses
 * Objekt [SettingsStore.onDeviceAiEnabled] auch selbst, sobald das Modell wirklich
 * verfügbar ist — unabhängig davon, ob der Screen noch sichtbar ist.
 */
object OnDeviceAiDownload {

    sealed interface State {
        data object Idle : State
        data object Checking : State

        /** [totalBytes] bleibt 0, solange AICore die Gesamtgröße noch nicht gemeldet hat. */
        data class Downloading(val downloadedBytes: Long = 0L, val totalBytes: Long = 0L) : State
        data object Ready : State

        /** Gerät unterstützt Gemini Nano nicht. */
        data object Unavailable : State
        data object Failed : State
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private var job: Job? = null

    /** Prüft die Verfügbarkeit und lädt das Modell bei Bedarf; erneute Aufrufe während eines laufenden Downloads sind wirkungslos. */
    fun start(store: SettingsStore) {
        if (job?.isActive == true) return
        job = scope.launch {
            _state.value = State.Checking
            when (runCatching { GenAiInsightsEngine.checkStatus() }.getOrNull()) {
                FeatureStatus.AVAILABLE -> enable(store)
                FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> {
                    _state.value = State.Downloading()
                    withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS) { runCatching { collectDownload() } }
                    // runCatching oben schluckt auch eine Abbruch-Exception; ohne diese
                    // Prüfung meldete ein per reset() abgebrochener Download fälschlich
                    // einen Fehler.
                    ensureActive()
                    // Maßgeblich ist am Ende allein der echte Status: AICore meldet den
                    // Abschluss nicht auf jedem Gerät zuverlässig über den Flow.
                    val available = runCatching { GenAiInsightsEngine.checkStatus() == FeatureStatus.AVAILABLE }
                        .getOrDefault(false)
                    if (available) enable(store) else _state.value = State.Failed
                }
                else -> _state.value = State.Unavailable
            }
        }
    }

    /** Setzt den Zustand zurück, z. B. wenn der Nutzer den Schalter wieder ausschaltet. */
    fun reset() {
        job?.cancel()
        job = null
        _state.value = State.Idle
    }

    private fun enable(store: SettingsStore) {
        store.onDeviceAiEnabled = true
        _state.value = State.Ready
    }

    private suspend fun collectDownload() {
        merge(GenAiInsightsEngine.download(), availabilityPolls())
            .transformWhile { status ->
                emit(status)
                status is DownloadStatus.DownloadStarted || status is DownloadStatus.DownloadProgress
            }
            .collect { status ->
                when (status) {
                    is DownloadStatus.DownloadStarted ->
                        _state.update { State.Downloading(totalBytes = status.bytesToDownload) }
                    is DownloadStatus.DownloadProgress ->
                        _state.update {
                            (it as? State.Downloading ?: State.Downloading())
                                .copy(downloadedBytes = status.totalBytesDownloaded)
                        }
                    else -> Unit
                }
            }
    }

    /**
     * Notausgang gegen den hängenden Fortschritt: läuft der Download bereits (etwa weil
     * eine andere App oder ein früherer Versuch ihn angestoßen hat), liefert der Flow von
     * AICore unter Umständen keinen Abschluss mehr. Der Poll beendet das Sammeln, sobald
     * das Modell tatsächlich einsatzbereit ist.
     */
    private fun availabilityPolls(): Flow<DownloadStatus> = flow {
        while (true) {
            delay(POLL_INTERVAL_MS)
            val available = runCatching { GenAiInsightsEngine.checkStatus() == FeatureStatus.AVAILABLE }
                .getOrDefault(false)
            if (available) {
                emit(DownloadStatus.DownloadCompleted)
                return@flow
            }
        }
    }

    private const val POLL_INTERVAL_MS = 3_000L
    private const val DOWNLOAD_TIMEOUT_MS = 15 * 60 * 1000L
}
