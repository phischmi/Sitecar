package app.papra.uploader.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.papra.uploader.R
import app.papra.uploader.data.PapraClient
import app.papra.uploader.data.SettingsStore
import app.papra.uploader.data.ThemeMode
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    store: SettingsStore,
    client: PapraClient,
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

}
