package app.papra.uploader.ui.scan

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.papra.uploader.R
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onScanned: (pages: List<File>) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var launching by remember { mutableStateOf(false) }

    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(false)
            .setPageLimit(20)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }
    val scanner = remember { GmsDocumentScanning.getClient(scannerOptions) }

    val resultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        launching = false
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val pageUris = scanResult?.pages?.mapNotNull { it.imageUri }.orEmpty()
            if (pageUris.isNotEmpty()) {
                runCatching { pageUris.map { copyUriToCache(context, it) } }
                    .onSuccess(onScanned)
                    .onFailure { errorMessage = it.message ?: "Konnte Scans nicht laden" }
            } else {
                errorMessage = "Scan lieferte keine Seiten."
            }
        }
        // RESULT_CANCELED: still on screen, user can re-trigger.
    }

    val startScan: () -> Unit = startScan@{
        if (launching) return@startScan
        if (activity == null) {
            errorMessage = "Activity-Kontext nicht verfügbar"
            return@startScan
        }
        launching = true
        errorMessage = null
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                resultLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                launching = false
                errorMessage = e.message ?: "Scanner konnte nicht gestartet werden"
            }
    }

    LaunchedEffect(Unit) { startScan() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.capture_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings))
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.DocumentScanner,
                contentDescription = null,
                modifier = Modifier.height(96.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.scan_hint),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = startScan,
                enabled = !launching,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(),
            ) {
                Text(stringResource(R.string.scan_start))
            }
        }
    }
}

private fun copyUriToCache(context: Context, uri: Uri): File {
    val dir = File(context.cacheDir, "scans").apply { mkdirs() }
    val ts = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    val out = File(dir, "papra-$ts.jpg")
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "Konnte URI nicht öffnen: $uri" }
        out.outputStream().use { output -> input.copyTo(output) }
    }
    return out
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
