package app.sitecar.uploader.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.sitecar.uploader.Features
import app.sitecar.uploader.R
import app.sitecar.uploader.data.AccentColor
import app.sitecar.uploader.data.BillingManager
import app.sitecar.uploader.data.SitecarApiClient
import app.sitecar.uploader.data.SettingsStore
import app.sitecar.uploader.data.SwipeAction
import app.sitecar.uploader.data.ThemeMode
import app.sitecar.uploader.icon.LauncherIcon
import app.sitecar.uploader.share.ShareReceiver
import app.sitecar.uploader.ui.support.SupportDialog
import app.sitecar.uploader.ui.theme.accentPrimaryColor
import app.sitecar.uploader.ui.util.ApiErrorMessages
import app.sitecar.uploader.ui.util.friendlyErrorMessage
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    store: SettingsStore,
    client: SitecarApiClient,
    billing: BillingManager,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    var serverUrl by remember { mutableStateOf(store.serverUrl) }
    var apiKey by remember { mutableStateOf(store.apiKey) }
    var status by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var orgCount by remember { mutableStateOf<Int?>(null) }
    var themeMode by remember { mutableStateOf(store.themeMode) }
    var ocrEnabled by remember { mutableStateOf(store.onDeviceOcrEnabled) }
    var shareIntentEnabled by remember { mutableStateOf(store.shareIntentEnabled) }
    var smartInsightsEnabled by remember { mutableStateOf(store.smartInsightsEnabled) }
    var filenameTemplate by remember { mutableStateOf(store.filenameTemplate) }
    var showFilenameHelp by remember { mutableStateOf(false) }
    var swipeStartToEndAction by remember { mutableStateOf(store.swipeStartToEndAction) }
    var swipeEndToStartAction by remember { mutableStateOf(store.swipeEndToStartAction) }
    var accentColor by remember { mutableStateOf(store.accentColor) }
    var showSupportDialog by remember { mutableStateOf(false) }
    val isSupporter by store.isSupporterFlow.collectAsState(initial = store.isSupporter)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val missingFieldsMessage = stringResource(R.string.settings_error_missing_fields)
    val apiErrorMessages = ApiErrorMessages(
        unauthorized = stringResource(R.string.error_unauthorized),
        forbidden = stringResource(R.string.error_forbidden),
        notFound = stringResource(R.string.error_not_found),
        server = stringResource(R.string.error_server),
        noConnection = stringResource(R.string.error_no_connection),
        timeout = stringResource(R.string.error_timeout),
        unknown = stringResource(R.string.error_unknown),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_theme_title),
                style = MaterialTheme.typography.labelLarge,
            )
            val themeOptions = listOf(
                ThemeMode.LIGHT to R.string.settings_theme_light,
                ThemeMode.DARK to R.string.settings_theme_dark,
                ThemeMode.SYSTEM to R.string.settings_theme_system,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                themeOptions.forEachIndexed { index, (mode, labelRes) ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size),
                        selected = themeMode == mode,
                        onClick = {
                            themeMode = mode
                            store.themeMode = mode
                        },
                    ) {
                        Text(stringResource(labelRes))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_ocr_title),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.settings_ocr_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = ocrEnabled,
                    onCheckedChange = {
                        ocrEnabled = it
                        store.onDeviceOcrEnabled = it
                    },
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_share_intent_title),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.settings_share_intent_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = shareIntentEnabled,
                    onCheckedChange = {
                        shareIntentEnabled = it
                        store.shareIntentEnabled = it
                        ShareReceiver.setEnabled(context, it)
                    },
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_smart_insights_title),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.settings_smart_insights_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = smartInsightsEnabled,
                    onCheckedChange = {
                        smartInsightsEnabled = it
                        store.smartInsightsEnabled = it
                    },
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.settings_filename_template_title),
                    style = MaterialTheme.typography.labelLarge,
                )
                IconButton(onClick = { showFilenameHelp = true }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.HelpOutline,
                        contentDescription = stringResource(R.string.settings_filename_template_help),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            OutlinedTextField(
                value = filenameTemplate,
                onValueChange = {
                    filenameTemplate = it
                    store.filenameTemplate = it
                },
                placeholder = { Text(stringResource(R.string.settings_filename_template_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_swipe_title),
                style = MaterialTheme.typography.labelLarge,
            )
            SwipeActionPicker(
                label = stringResource(R.string.settings_swipe_start_to_end),
                selected = swipeStartToEndAction,
                onSelected = {
                    swipeStartToEndAction = it
                    store.swipeStartToEndAction = it
                },
            )
            Spacer(Modifier.height(8.dp))
            SwipeActionPicker(
                label = stringResource(R.string.settings_swipe_end_to_start),
                selected = swipeEndToStartAction,
                onSelected = {
                    swipeEndToStartAction = it
                    store.swipeEndToStartAction = it
                },
            )

            // Akzentfarben-Auswahl und Unterstützer-Bereich lassen sich unabhängig
            // von der (vorerst deaktivierten) Bezahlfunktion zum Testen/Vorschauen
            // freischalten, siehe Features.SUPPORTER_PREVIEW_ENABLED.
            if (Features.SUPPORTER_ENABLED || Features.SUPPORTER_PREVIEW_ENABLED) {
                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.settings_accent_title),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = stringResource(R.string.settings_accent_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AccentColor.entries.forEach { accent ->
                        // Solange die Bezahlfunktion aus ist, sind alle Farben zum
                        // Testen frei nutzbar; sobald sie aktiviert wird, greift
                        // automatisch wieder die echte Unterstützer-Sperre.
                        val unlocked = accent == AccentColor.INDIGO || isSupporter || !Features.SUPPORTER_ENABLED
                        AccentSwatch(
                            accent = accent,
                            selected = accentColor == accent,
                            unlocked = unlocked,
                            onClick = {
                                if (unlocked) {
                                    accentColor = accent
                                    store.accentColor = accent
                                    LauncherIcon.apply(context, accent)
                                } else {
                                    showSupportDialog = true
                                }
                            },
                        )
                    }
                }
                if (!isSupporter && Features.SUPPORTER_ENABLED) {
                    Text(
                        text = stringResource(R.string.settings_accent_locked_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (Features.SUPPORTER_ENABLED || Features.SUPPORTER_PREVIEW_ENABLED) {
                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.settings_support_title),
                    style = MaterialTheme.typography.labelLarge,
                )
                // Ohne echte Bezahlfunktion gibt es kein In-App-Produkt zum Kaufen —
                // die Vorschau zeigt daher immer den "bereits Unterstützer"-Zustand
                // statt einer Kauf-CTA, die ins Leere liefe.
                val previewingAsSupporter = Features.SUPPORTER_PREVIEW_ENABLED && !Features.SUPPORTER_ENABLED
                if (isSupporter || previewingAsSupporter) {
                    Text(
                        text = stringResource(R.string.settings_support_badge),
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    OutlinedButton(
                        onClick = { showSupportDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_support_cta))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text(stringResource(R.string.settings_server_url)) },
                placeholder = { Text(stringResource(R.string.settings_server_url_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text(stringResource(R.string.settings_api_key)) },
                placeholder = { Text(stringResource(R.string.settings_api_key_hint)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))

            OutlinedButton(
                onClick = {
                    if (serverUrl.isBlank() || apiKey.isBlank()) {
                        isError = true
                        status = missingFieldsMessage
                        return@OutlinedButton
                    }
                    loading = true
                    scope.launch {
                        val res = client.listOrganizations(
                            overrideUrl = serverUrl.trim(),
                            overrideKey = apiKey.trim(),
                        )
                        loading = false
                        res.onSuccess { orgs ->
                            isError = false
                            orgCount = orgs.size
                            status = null
                        }.onFailure { e ->
                            isError = true
                            orgCount = null
                            status = friendlyErrorMessage(e, apiErrorMessages)
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.height(20.dp),
                    )
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
            status?.let {
                Text(
                    text = if (isError) stringResource(R.string.settings_test_failed, it) else it,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    store.serverUrl = serverUrl
                    store.apiKey = apiKey
                    onSaved()
                },
                enabled = serverUrl.isNotBlank() && apiKey.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_save))
            }
        }
    }

    if (Features.SUPPORTER_ENABLED && showSupportDialog) {
        SupportDialog(
            billing = billing,
            onDismiss = { showSupportDialog = false },
        )
    }

    if (showFilenameHelp) {
        AlertDialog(
            onDismissRequest = { showFilenameHelp = false },
            title = { Text(stringResource(R.string.settings_filename_template_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilenamePlaceholderRow("{date}", stringResource(R.string.settings_filename_placeholder_date))
                    FilenamePlaceholderRow("{time}", stringResource(R.string.settings_filename_placeholder_time))
                    FilenamePlaceholderRow("{org}", stringResource(R.string.settings_filename_placeholder_org))
                    FilenamePlaceholderRow("{counter}", stringResource(R.string.settings_filename_placeholder_counter))
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilenameHelp = false }) {
                    Text(stringResource(R.string.action_done))
                }
            },
        )
    }
}

@Composable
private fun FilenamePlaceholderRow(placeholder: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = placeholder,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(84.dp),
        )
        Text(text = description, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Zeigt eine Akzentfarbe als Kreis, diagonal geteilt in Hell- und
 * Dunkelmodus-Farbe (oben-links = Hell, unten-rechts = Dunkel) — so ist auf
 * einen Blick sichtbar, wie der Akzent in beiden Darstellungen aussieht,
 * unabhängig vom aktuell aktiven App-Theme.
 */
@Composable
private fun AccentSwatch(
    accent: AccentColor,
    selected: Boolean,
    unlocked: Boolean,
    onClick: () -> Unit,
) {
    val lightColor = accentPrimaryColor(accent, darkTheme = false)
    val darkColor = accentPrimaryColor(accent, darkTheme = true)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            drawPath(
                path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w, 0f)
                    lineTo(0f, h)
                    close()
                },
                color = lightColor,
            )
            drawPath(
                path = Path().apply {
                    moveTo(w, 0f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                },
                color = darkColor,
            )
        }
        if (!unlocked) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeActionPicker(
    label: String,
    selected: SwipeAction,
    onSelected: (SwipeAction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = swipeActionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            SwipeAction.entries.forEach { action ->
                DropdownMenuItem(
                    text = { Text(swipeActionLabel(action)) },
                    onClick = {
                        onSelected(action)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun swipeActionLabel(action: SwipeAction): String = when (action) {
    SwipeAction.NONE -> stringResource(R.string.settings_swipe_action_none)
    SwipeAction.DELETE -> stringResource(R.string.action_delete)
    SwipeAction.RENAME -> stringResource(R.string.action_rename)
    SwipeAction.EDIT_TAGS -> stringResource(R.string.action_manage_tags)
}
