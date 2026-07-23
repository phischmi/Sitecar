package app.sitecar.uploader.ui.upload

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.sitecar.uploader.Features
import app.sitecar.uploader.R
import app.sitecar.uploader.data.BillingManager
import app.sitecar.uploader.data.FilenameTemplate
import app.sitecar.uploader.data.Organization
import app.sitecar.uploader.data.PdfTextExtractor
import app.sitecar.uploader.data.PendingUpload
import app.sitecar.uploader.data.SitecarApiClient
import app.sitecar.uploader.data.PdfBuilder
import app.sitecar.uploader.data.SettingsStore
import app.sitecar.uploader.data.TagDto
import app.sitecar.uploader.data.duplicates.PerceptualHash
import app.sitecar.uploader.data.duplicates.RecentUpload
import app.sitecar.uploader.data.duplicates.RecentUploadsStore
import app.sitecar.uploader.data.insights.Deadline
import app.sitecar.uploader.data.insights.DocumentInsights
import app.sitecar.uploader.data.insights.RuleBasedInsightsEngine
import app.sitecar.uploader.data.reminders.ReminderScheduler
import app.sitecar.uploader.ui.support.SupportDialog
import app.sitecar.uploader.ui.util.ApiErrorMessages
import app.sitecar.uploader.ui.util.friendlyErrorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    pendingUpload: PendingUpload?,
    client: SitecarApiClient,
    pdfBuilder: PdfBuilder,
    store: SettingsStore,
    billing: BillingManager,
    recentUploads: RecentUploadsStore,
    onDone: () -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    if (pendingUpload == null) {
        // Screen ohne State erreicht (z. B. Prozess-Restart) – einfach zurück.
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var orgs by remember { mutableStateOf<List<Organization>>(emptyList()) }
    var selectedOrg by remember { mutableStateOf<Organization?>(null) }
    var fileName by remember { mutableStateOf("") }
    var orgsLoading by remember { mutableStateOf(true) }
    var dropdownOpen by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    val pdfBuildFailedMessage = stringResource(R.string.upload_pdf_build_failed)
    val apiErrorMessages = ApiErrorMessages(
        unauthorized = stringResource(R.string.error_unauthorized),
        notFound = stringResource(R.string.error_not_found),
        server = stringResource(R.string.error_server),
        noConnection = stringResource(R.string.error_no_connection),
        timeout = stringResource(R.string.error_timeout),
        unknown = stringResource(R.string.error_unknown),
    )

    var documentFile by remember { mutableStateOf<File?>(null) }
    var documentMimeType by remember { mutableStateOf("application/pdf") }
    var pdfProgressCurrent by remember { mutableIntStateOf(0) }
    val totalPages = (pendingUpload as? PendingUpload.Images)?.pages?.size ?: 1

    var uploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var insights by remember { mutableStateOf(DocumentInsights.EMPTY) }
    var orgTags by remember { mutableStateOf<List<TagDto>>(emptyList()) }
    var selectedTagIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingDeadline by remember { mutableStateOf<Deadline?>(null) }
    var pendingDeadlineDocumentName by remember { mutableStateOf("") }
    var pendingShouldPromptSupport by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {},
    )

    val scope = rememberCoroutineScope()
    val previewBitmap = remember(pendingUpload) {
        when (pendingUpload) {
            is PendingUpload.Images ->
                runCatching { android.graphics.BitmapFactory.decodeFile(pendingUpload.pages.first().absolutePath) }
                    .getOrNull()
            is PendingUpload.ReadyDocument -> when {
                pendingUpload.mimeType.startsWith("image/") ->
                    runCatching { android.graphics.BitmapFactory.decodeFile(pendingUpload.file.absolutePath) }
                        .getOrNull()
                pendingUpload.mimeType == "application/pdf" ->
                    runCatching { renderPdfFirstPage(pendingUpload.file) }.getOrNull()
                else -> null
            }
        }
    }

    var imageHash by remember { mutableStateOf<Long?>(null) }
    var duplicateWarning by remember { mutableStateOf<RecentUpload?>(null) }

    LaunchedEffect(previewBitmap) {
        val bitmap = previewBitmap ?: return@LaunchedEffect
        imageHash = withContext(Dispatchers.Default) { PerceptualHash.compute(bitmap) }
    }

    LaunchedEffect(imageHash, selectedOrg) {
        val hash = imageHash ?: return@LaunchedEffect
        val org = selectedOrg ?: return@LaunchedEffect
        duplicateWarning = withContext(Dispatchers.Default) { recentUploads.findSimilar(org.id, hash) }
    }

    LaunchedEffect(Unit) {
        client.listOrganizations()
            .onSuccess {
                orgs = it
                selectedOrg = it.firstOrNull()
            }
            .onFailure { errorMessage = friendlyErrorMessage(it, apiErrorMessages) }
        orgsLoading = false
    }

    LaunchedEffect(selectedOrg) {
        val org = selectedOrg ?: return@LaunchedEffect
        if (store.smartInsightsEnabled) {
            client.listTags(org.id).onSuccess { orgTags = it }
        }
    }

    LaunchedEffect(documentFile) {
        val doc = documentFile ?: return@LaunchedEffect
        if (!store.smartInsightsEnabled) return@LaunchedEffect
        val text = when (pendingUpload) {
            is PendingUpload.Images -> if (store.onDeviceOcrEnabled) {
                PdfTextExtractor.extractText(doc)
            } else {
                pdfBuilder.recognizeText(pendingUpload.pages)
            }
            is PendingUpload.ReadyDocument ->
                if (pendingUpload.mimeType == "application/pdf") PdfTextExtractor.extractText(doc) else ""
        }
        insights = RuleBasedInsightsEngine.analyze(text)
    }

    LaunchedEffect(insights, orgTags) {
        selectedTagIds = orgTags
            .filter { tag -> insights.suggestedTagNames.any { it.equals(tag.name, ignoreCase = true) } }
            .map { it.id }
            .toSet()
    }

    LaunchedEffect(selectedOrg) {
        if (fileName.isBlank()) {
            fileName = when (pendingUpload) {
                is PendingUpload.Images -> FilenameTemplate.render(
                    template = store.filenameTemplate,
                    organizationName = selectedOrg?.name,
                    counter = store.uploadCount + 1,
                ) + ".pdf"
                is PendingUpload.ReadyDocument -> pendingUpload.suggestedName?.takeIf { it.isNotBlank() }
                    ?: FilenameTemplate.render(
                        template = store.filenameTemplate,
                        organizationName = selectedOrg?.name,
                        counter = store.uploadCount + 1,
                    ) + "." + extensionForMimeType(pendingUpload.mimeType)
            }
        }
    }

    LaunchedEffect(pendingUpload) {
        when (pendingUpload) {
            is PendingUpload.Images -> {
                val outFile = File(
                    File(pendingUpload.pages.first().parentFile?.parentFile, "pdfs").apply { mkdirs() },
                    "sitecar-${System.currentTimeMillis()}.pdf",
                )
                val res = withContext(Dispatchers.IO) {
                    pdfBuilder.build(
                        imageFiles = pendingUpload.pages,
                        outputFile = outFile,
                        ocrEnabled = store.onDeviceOcrEnabled,
                        onProgress = { current, _ -> pdfProgressCurrent = current },
                    )
                }
                res.onSuccess {
                    documentFile = it
                    documentMimeType = "application/pdf"
                }.onFailure { errorMessage = it.message ?: pdfBuildFailedMessage }
            }
            is PendingUpload.ReadyDocument -> {
                documentFile = pendingUpload.file
                documentMimeType = pendingUpload.mimeType
            }
        }
    }

    val documentReady = documentFile != null
    val isReadyDocument = pendingUpload is PendingUpload.ReadyDocument

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.upload_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
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
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } else if (isReadyDocument) {
                    Icon(
                        imageVector = Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        modifier = Modifier.height(64.dp),
                    )
                }
                if (totalPages > 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.upload_pages_count, totalPages),
                            color = Color.White,
                        )
                    }
                }
            }

            if (!documentReady && pendingUpload is PendingUpload.Images) {
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

            if (pendingUpload is PendingUpload.ReadyDocument && pendingUpload.ignoredCount > 0) {
                Text(
                    text = stringResource(
                        R.string.upload_shared_multiple_ignored,
                        pendingUpload.ignoredCount + 1,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

            insights.documentDate?.let { docDate ->
                AssistChip(
                    onClick = {
                        val extension = if (pendingUpload is PendingUpload.Images) {
                            "pdf"
                        } else {
                            extensionForMimeType(documentMimeType)
                        }
                        fileName = FilenameTemplate.render(
                            template = store.filenameTemplate,
                            organizationName = selectedOrg?.name,
                            counter = store.uploadCount + 1,
                            documentDate = docDate,
                        ) + ".$extension"
                    },
                    label = { Text(stringResource(R.string.upload_use_document_date, docDate.format(DISPLAY_DATE_FORMATTER))) },
                )
            }

            val suggestedTags = orgTags.filter { tag ->
                insights.suggestedTagNames.any { it.equals(tag.name, ignoreCase = true) }
            }
            if (suggestedTags.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.upload_suggested_tags),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        suggestedTags.forEach { tag ->
                            FilterChip(
                                selected = tag.id in selectedTagIds,
                                onClick = {
                                    selectedTagIds = if (tag.id in selectedTagIds) {
                                        selectedTagIds - tag.id
                                    } else {
                                        selectedTagIds + tag.id
                                    }
                                },
                                label = { Text(tag.name) },
                            )
                        }
                    }
                }
            }

            duplicateWarning?.let { duplicate ->
                val uploadedDate = Instant.ofEpochMilli(duplicate.uploadedAtEpochMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                Text(
                    text = stringResource(
                        R.string.upload_duplicate_warning,
                        duplicate.fileName,
                        uploadedDate.format(DISPLAY_DATE_FORMATTER),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

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
                    val doc = documentFile ?: return@onClick
                    uploading = true
                    errorMessage = null
                    scope.launch {
                        val finalName = fileName.trim().ifBlank { doc.name }.let { raw ->
                            if (pendingUpload is PendingUpload.Images) {
                                if (raw.endsWith(".pdf", ignoreCase = true)) raw else "$raw.pdf"
                            } else {
                                ensureExtension(raw, documentMimeType)
                            }
                        }
                        val res = withContext(Dispatchers.IO) {
                            client.uploadDocument(
                                organizationId = org.id,
                                file = doc,
                                fileName = finalName,
                                mimeType = documentMimeType,
                            )
                        }
                        uploading = false
                        val uploadedDoc = res.getOrNull()
                        if (uploadedDoc == null) {
                            errorMessage = friendlyErrorMessage(
                                res.exceptionOrNull() ?: IllegalStateException("Upload failed"),
                                apiErrorMessages,
                            )
                            return@launch
                        }

                        // Cleanup: erzeugtes/übernommenes Dokument und ggf. Quell-JPEGs löschen.
                        doc.delete()
                        (pendingUpload as? PendingUpload.Images)?.pages?.forEach { it.delete() }

                        if (selectedTagIds.isNotEmpty()) {
                            withContext(Dispatchers.IO) {
                                selectedTagIds.forEach { tagId ->
                                    runCatching { client.addTagToDocument(org.id, uploadedDoc.id, tagId) }
                                }
                            }
                        }

                        imageHash?.let { hash ->
                            recentUploads.record(
                                RecentUpload(
                                    organizationId = org.id,
                                    hash = hash,
                                    fileName = finalName,
                                    uploadedAtEpochMillis = System.currentTimeMillis(),
                                ),
                            )
                        }

                        store.uploadCount += 1
                        val count = store.uploadCount
                        val shouldPromptSupport = Features.SUPPORTER_ENABLED &&
                            !store.isSupporter &&
                            (count == 5 || (count > 5 && (count - 5) % 10 == 0))

                        val deadline = insights.deadline
                        when {
                            deadline != null && store.smartInsightsEnabled -> {
                                pendingDeadlineDocumentName = finalName
                                pendingShouldPromptSupport = shouldPromptSupport
                                pendingDeadline = deadline
                            }
                            shouldPromptSupport -> showSupportDialog = true
                            else -> onDone()
                        }
                    }
                },
                enabled = !uploading && documentReady && selectedOrg != null && fileName.isNotBlank(),
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
                Text(stringResource(if (isReadyDocument) R.string.upload_cancel else R.string.upload_retake))
            }
        }
    }

    pendingDeadline?.let { deadline ->
        AlertDialog(
            onDismissRequest = {
                pendingDeadline = null
                if (pendingShouldPromptSupport) showSupportDialog = true else onDone()
            },
            title = { Text(stringResource(R.string.upload_deadline_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.upload_deadline_dialog_message,
                        deadline.label,
                        deadline.date.format(DISPLAY_DATE_FORMATTER),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        ReminderScheduler.schedule(
                            context = context,
                            date = deadline.date,
                            label = deadline.label,
                            documentName = pendingDeadlineDocumentName,
                        )
                        pendingDeadline = null
                        if (pendingShouldPromptSupport) showSupportDialog = true else onDone()
                    },
                ) {
                    Text(stringResource(R.string.upload_deadline_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDeadline = null
                        if (pendingShouldPromptSupport) showSupportDialog = true else onDone()
                    },
                ) {
                    Text(stringResource(R.string.upload_deadline_dialog_dismiss))
                }
            },
        )
    }

    if (Features.SUPPORTER_ENABLED && showSupportDialog) {
        SupportDialog(
            billing = billing,
            onDismiss = {
                showSupportDialog = false
                onDone()
            },
        )
    }
}

private fun renderPdfFirstPage(file: File): android.graphics.Bitmap? {
    return try {
        android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount == 0) return null
                renderer.openPage(0).use { page ->
                    val bitmap = android.graphics.Bitmap.createBitmap(
                        page.width,
                        page.height,
                        android.graphics.Bitmap.Config.ARGB_8888,
                    )
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        }
    } catch (e: Exception) {
        null
    }
}

private val DISPLAY_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy", java.util.Locale.US)

private fun extensionForMimeType(mimeType: String): String = when {
    mimeType == "application/pdf" -> "pdf"
    mimeType.startsWith("image/") -> mimeType.substringAfter('/').ifBlank { "jpg" }
    else -> "bin"
}

private fun ensureExtension(name: String, mimeType: String): String {
    if (name.contains('.')) return name
    return "$name.${extensionForMimeType(mimeType)}"
}
