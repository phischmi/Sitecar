package app.papra.uploader.ui.upload

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.papra.uploader.R
import app.papra.uploader.data.Organization
import app.papra.uploader.data.PapraClient
import app.papra.uploader.data.PdfBuilder
import app.papra.uploader.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    pages: List<File>,
    client: PapraClient,
    pdfBuilder: PdfBuilder,
    store: SettingsStore,
    onDone: () -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    if (pages.isEmpty()) {
        // Falls UploadScreen ohne State erreicht wird (Prozess-Restart) – einfach zurück.
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var orgs by remember { mutableStateOf<List<Organization>>(emptyList()) }
    var selectedOrg by remember { mutableStateOf<Organization?>(null) }
    var fileName by remember {
        mutableStateOf(
            "Scan-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}.pdf",
        )
    }
    var orgsLoading by remember { mutableStateOf(true) }
    var dropdownOpen by remember { mutableStateOf(false) }

    var pdfFile by remember { mutableStateOf<File?>(null) }
    var pdfProgressCurrent by remember { mutableIntStateOf(0) }
    val totalPages = pages.size

    var uploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val firstPageThumbnail = remember(pages) {
        runCatching { android.graphics.BitmapFactory.decodeFile(pages.first().absolutePath) }
            .getOrNull()
    }

    LaunchedEffect(Unit) {
        client.listOrganizations()
            .onSuccess {
                orgs = it
                selectedOrg = it.firstOrNull()
            }
            .onFailure { errorMessage = it.message }
        orgsLoading = false
    }

    LaunchedEffect(pages) {
        val outFile = File(
            File(pages.first().parentFile?.parentFile, "pdfs").apply { mkdirs() },
            "papra-${System.currentTimeMillis()}.pdf",
        )
        val res = withContext(Dispatchers.IO) {
            pdfBuilder.build(
                imageFiles = pages,
                outputFile = outFile,
                ocrEnabled = store.onDeviceOcrEnabled,
                onProgress = { current, _ -> pdfProgressCurrent = current },
            )
        }
        res.onSuccess { pdfFile = it }
            .onFailure { errorMessage = it.message ?: "PDF konnte nicht erstellt werden" }
    }

    val pdfReady = pdfFile != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.upload_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.nav_settings),
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                firstPageThumbnail?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
                if (totalPages > 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color(0x99000000), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.upload_pages_count, totalPages),
                            color = Color.White,
                        )
                    }
                }
            }

            if (!pdfReady) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(
                            if (store.onDeviceOcrEnabled) {
                                R.string.upload_pdf_progress
                            } else {
                                R.string.upload_pdf_progress_no_ocr
                            },
                            pdfProgressCurrent.coerceAtLeast(1),
                            totalPages,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LinearProgressIndicator(
                        progress = {
                            if (totalPages == 0) 0f else pdfProgressCurrent.toFloat() / totalPages
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            ExposedDropdownMenuBox(
                expanded = dropdownOpen,
                onExpandedChange = { dropdownOpen = !dropdownOpen },
            ) {
                OutlinedTextField(
                    value = selectedOrg?.name ?: if (orgsLoading) "…" else "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.upload_org)) },
                    trailingIcon = { Icon(Icons.Default.ExpandMore, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                )
                DropdownMenu(
                    expanded = dropdownOpen,
                    onDismissRequest = { dropdownOpen = false },
                ) {
                    orgs.forEach { org ->
                        DropdownMenuItem(
                            text = { Text(org.name) },
                            onClick = {
                                selectedOrg = org
                                dropdownOpen = false
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = fileName,
                onValueChange = { fileName = it },
                label = { Text(stringResource(R.string.upload_filename)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            errorMessage?.let {
                Text(
                    text = stringResource(R.string.upload_failed, it),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onClick@{
                    val org = selectedOrg ?: return@onClick
                    val pdf = pdfFile ?: return@onClick
                    uploading = true
                    errorMessage = null
                    scope.launch {
                        val finalName = fileName.trim().ifBlank { pdf.name }
                            .let { if (it.endsWith(".pdf", ignoreCase = true)) it else "$it.pdf" }
                        val res = withContext(Dispatchers.IO) {
                            client.uploadDocument(
                                organizationId = org.id,
                                file = pdf,
                                fileName = finalName,
                                mimeType = "application/pdf",
                            )
                        }
                        uploading = false
                        res.onSuccess {
                            // Cleanup: PDF und Quell-JPEGs löschen.
                            pdf.delete()
                            pages.forEach { it.delete() }
                            onDone()
                        }.onFailure { e ->
                            errorMessage = e.message
                        }
                    }
                },
                enabled = !uploading && pdfReady && selectedOrg != null && fileName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uploading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.height(20.dp),
                    )
                } else {
                    Text(stringResource(R.string.upload_button))
                }
            }

            OutlinedButton(
                onClick = onBack,
                enabled = !uploading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.upload_retake))
            }
        }
    }
}
