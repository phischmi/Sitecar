package app.sitecar.uploader.ui.settings

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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.sitecar.uploader.R
import app.sitecar.uploader.data.AccentColor
import app.sitecar.uploader.data.BillingManager
import app.sitecar.uploader.data.PapraClient
import app.sitecar.uploader.data.SettingsStore
import app.sitecar.uploader.data.ThemeMode
import app.sitecar.uploader.icon.LauncherIcon
import app.sitecar.uploader.ui.support.SupportDialog
import app.sitecar.uploader.ui.theme.accentPrimaryColor
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    store: SettingsStore,
    client: PapraClient,
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
    var filenameTemplate by remember { mutableStateOf(store.filenameTemplate) }
    var accentColor by remember { mutableStateOf(store.accentColor) }
    var showSupportDialog by remember { mutableStateOf(false) }
    val isSupporter by store.isSupporterFlow.collectAsState(initial = store.isSupporter)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val missingFieldsMessage = stringResource(R.string.settings_error_missing_fields)
    val unknownErrorMessage = stringResource(R.string.error_unknown)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
                text = stringResource(R.string.settings_filename_template_title),
                style = MaterialTheme.typography.labelLarge,
            )
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
                text = stringResource(R.string.settings_accent_title),
                style = MaterialTheme.typography.labelLarge,
            )
            val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AccentColor.entries.forEach { accent ->
                    val unlocked = accent == AccentColor.INDIGO || isSupporter
                    val swatchColor = accentPrimaryColor(accent, isDarkTheme)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .then(
                                if (accentColor == accent) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                } else {
                                    Modifier
                                },
                            )
                            .clickable {
                                if (unlocked) {
                                    accentColor = accent
                                    store.accentColor = accent
                                    LauncherIcon.apply(context, accent)
                                } else {
                                    showSupportDialog = true
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
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
            }
            if (!isSupporter) {
                Text(
                    text = stringResource(R.string.settings_accent_locked_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_support_title),
                style = MaterialTheme.typography.labelLarge,
            )
            if (isSupporter) {
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
                            status = e.message ?: unknownErrorMessage
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

    if (showSupportDialog) {
        SupportDialog(
            billing = billing,
            onDismiss = { showSupportDialog = false },
        )
    }
}
