package app.sitecar.client.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sitecar.client.R
import app.sitecar.client.data.SettingsStore
import app.sitecar.client.data.SitecarApiClient
import app.sitecar.client.ui.util.friendlyErrorMessage
import app.sitecar.client.ui.util.rememberApiErrorMessages
import app.sitecar.client.ui.util.scaffoldContentInsets
import kotlinx.coroutines.launch

private enum class OnboardingStep { WELCOME, CONNECTION, TOUR }

private val ICON_SIZE = 96.dp

/** Verhältnis von Gesamtfläche (108 dp) zur sichtbaren Fläche (72 dp) eines Adaptive Icons. */
private const val ADAPTIVE_ICON_SCALE = 1.5f

private data class TourPage(val icon: ImageVector, val titleRes: Int, val bodyRes: Int)

/**
 * Die Tour folgt den Tabs der App und benutzt bewusst dieselben Symbole wie die
 * untere Navigationsleiste, damit die Seiten beim ersten Blick in die App
 * wiedererkennbar sind. Die letzte Seite fällt aus dem Schema: die intelligenten
 * Vorschläge stecken im Upload und haben keinen eigenen Tab.
 */
private val TOUR_PAGES = listOf(
    TourPage(
        icon = Icons.Default.DocumentScanner,
        titleRes = R.string.onboarding_tour_scan_title,
        bodyRes = R.string.onboarding_tour_scan_body,
    ),
    TourPage(
        icon = Icons.Default.Description,
        titleRes = R.string.onboarding_tour_documents_title,
        bodyRes = R.string.onboarding_tour_documents_body,
    ),
    TourPage(
        icon = Icons.Default.Sell,
        titleRes = R.string.onboarding_tour_tags_title,
        bodyRes = R.string.onboarding_tour_tags_body,
    ),
    TourPage(
        icon = Icons.Default.Delete,
        titleRes = R.string.onboarding_tour_trash_title,
        bodyRes = R.string.onboarding_tour_trash_body,
    ),
    TourPage(
        icon = Icons.Default.AutoAwesome,
        titleRes = R.string.onboarding_tour_insights_title,
        bodyRes = R.string.onboarding_tour_insights_body,
    ),
)

/**
 * Einstieg für neue Nutzer: Begrüßung, danach die Serververbindung und optional
 * eine kurze Tour. Bis auf die Verbindung stellt hier nichts etwas ein — die Tour
 * erklärt nur, was die App kann, und lässt sich schon auf der Begrüßungsseite
 * abwählen.
 */
@Composable
fun OnboardingScreen(
    store: SettingsStore,
    client: SitecarApiClient,
    onFinished: () -> Unit,
) {
    var step by remember { mutableStateOf(OnboardingStep.WELCOME) }
    var showTour by remember { mutableStateOf(true) }

    BackHandler(enabled = step != OnboardingStep.WELCOME) {
        step = if (step == OnboardingStep.TOUR) OnboardingStep.CONNECTION else OnboardingStep.WELCOME
    }

    Scaffold(contentWindowInsets = scaffoldContentInsets) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .consumeWindowInsets(inner)
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            when (step) {
                OnboardingStep.WELCOME -> WelcomeStep(
                    showTour = showTour,
                    onShowTourChange = { showTour = it },
                    onStart = { step = OnboardingStep.CONNECTION },
                )
                OnboardingStep.CONNECTION -> ConnectionStep(
                    store = store,
                    client = client,
                    onBack = { step = OnboardingStep.WELCOME },
                    onConnected = {
                        if (showTour) step = OnboardingStep.TOUR else onFinished()
                    },
                )
                OnboardingStep.TOUR -> TourStep(
                    onBack = { step = OnboardingStep.CONNECTION },
                    onFinished = onFinished,
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(
    showTour: Boolean,
    onShowTourChange: (Boolean) -> Unit,
    onStart: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        Spacer(Modifier.weight(1f))
        AppIcon()
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Checkbox(checked = showTour, onCheckedChange = onShowTourChange)
            Text(
                text = stringResource(R.string.onboarding_welcome_tour),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_welcome_start))
        }
    }
}

@Composable
private fun ConnectionStep(
    store: SettingsStore,
    client: SitecarApiClient,
    onBack: () -> Unit,
    onConnected: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val apiErrorMessages = rememberApiErrorMessages()
    val missingFieldsMessage = stringResource(R.string.settings_error_missing_fields)

    var serverUrl by remember { mutableStateOf(store.serverUrl) }
    var apiKey by remember { mutableStateOf(store.apiKey) }
    var testing by remember { mutableStateOf(false) }
    var orgCount by remember { mutableStateOf<Int?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_connect_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.onboarding_connect_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = serverUrl,
                onValueChange = {
                    serverUrl = it
                    orgCount = null
                },
                label = { Text(stringResource(R.string.settings_server_url)) },
                placeholder = { Text(stringResource(R.string.settings_server_url_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    orgCount = null
                },
                label = { Text(stringResource(R.string.settings_api_key)) },
                placeholder = { Text(stringResource(R.string.settings_api_key_hint)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = {
                    if (serverUrl.isBlank() || apiKey.isBlank()) {
                        orgCount = null
                        errorText = missingFieldsMessage
                        return@OutlinedButton
                    }
                    testing = true
                    errorText = null
                    scope.launch {
                        client.listOrganizations(
                            overrideUrl = serverUrl.trim(),
                            overrideKey = apiKey.trim(),
                        ).onSuccess { orgs ->
                            orgCount = orgs.size
                            errorText = null
                        }.onFailure { e ->
                            orgCount = null
                            errorText = friendlyErrorMessage(e, apiErrorMessages)
                        }
                        testing = false
                    }
                },
                enabled = !testing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (testing) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text(stringResource(R.string.settings_test))
                }
            }
            orgCount?.let {
                Text(
                    text = stringResource(R.string.settings_test_ok, it),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            errorText?.let {
                Text(
                    text = stringResource(R.string.settings_test_failed, it),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        StepButtons(
            onBack = onBack,
            forwardLabel = stringResource(R.string.onboarding_continue),
            forwardEnabled = serverUrl.isNotBlank() && apiKey.isNotBlank(),
            onForward = {
                store.serverUrl = serverUrl
                store.apiKey = apiKey
                onConnected()
            },
        )
    }
}

@Composable
private fun TourStep(
    onBack: () -> Unit,
    onFinished: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { TOUR_PAGES.size })

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val tourPage = TOUR_PAGES[page]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                StepIcon(tourPage.icon)
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(tourPage.titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(tourPage.bodyRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 16.dp),
        ) {
            repeat(TOUR_PAGES.size) { index ->
                val active = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (active) 10.dp else 8.dp)
                        .background(
                            color = if (active) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = CircleShape,
                        ),
                )
            }
        }

        val lastPage = pagerState.currentPage == TOUR_PAGES.lastIndex
        StepButtons(
            onBack = {
                if (pagerState.currentPage == 0) {
                    onBack()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }
            },
            forwardLabel = stringResource(
                if (lastPage) R.string.onboarding_finish else R.string.onboarding_continue,
            ),
            forwardEnabled = true,
            onForward = {
                if (lastPage) {
                    onFinished()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
        )
    }
}

/**
 * Das echte Launcher-Icon zur Begrüßung: die beiden Ebenen des Adaptive Icons
 * übereinander, im Kreis beschnitten wie im Launcher. Die Ebenen sind auf 108 dp
 * angelegt, sichtbar ist davon die mittlere Fläche von 72 dp — deshalb werden sie
 * um das Anderthalbfache des sichtbaren Kreises gezeichnet.
 */
@Composable
private fun AppIcon() {
    Box(
        modifier = Modifier
            .size(ICON_SIZE)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        val layerSize = ICON_SIZE * ADAPTIVE_ICON_SCALE
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = null,
            modifier = Modifier.size(layerSize),
        )
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(layerSize),
        )
    }
}

@Composable
private fun StepIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(ICON_SIZE)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(48.dp),
        )
    }
}

@Composable
private fun StepButtons(
    onBack: () -> Unit,
    forwardLabel: String,
    forwardEnabled: Boolean,
    onForward: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.onboarding_back))
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onForward, enabled = forwardEnabled) {
            Text(forwardLabel)
        }
    }
}
