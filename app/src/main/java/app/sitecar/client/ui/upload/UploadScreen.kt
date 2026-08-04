package app.sitecar.client.ui.upload

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import app.sitecar.client.Features
import app.sitecar.client.R
import app.sitecar.client.data.BillingManager
import app.sitecar.client.data.FilenameTemplate
import app.sitecar.client.data.Organization
import app.sitecar.client.data.PdfTextExtractor
import app.sitecar.client.data.PdfBuilder
import app.sitecar.client.data.PendingUpload
import app.sitecar.client.data.ReviewPrompt
import app.sitecar.client.data.SettingsStore
import app.sitecar.client.data.SitecarApiClient
import app.sitecar.client.data.TagDto
import app.sitecar.client.data.extensionForMimeType
import app.sitecar.client.data.renderPdfFirstPage
import app.sitecar.client.data.duplicates.PerceptualHash
import app.sitecar.client.data.duplicates.RecentUpload
import app.sitecar.client.data.duplicates.RecentUploadsStore
import app.sitecar.client.data.insights.DocumentInsights
import app.sitecar.client.data.insights.HybridInsightsEngine
import app.sitecar.client.data.insights.RuleBasedInsightsEngine
import app.sitecar.client.ui.support.SupportDialog
import app.sitecar.client.ui.util.findActivity
import app.sitecar.client.ui.util.friendlyErrorMessage
import app.sitecar.client.ui.util.rememberApiErrorMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    pendingUpload: PendingUpload?,
    client: SitecarApiClient,
    pdfBuilder: PdfBuilder,
    store: SettingsStore,
    billing: BillingManager,
    recentUploads: RecentUploadsStore,
    reviewPrompt: ReviewPrompt,
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
    val activity = LocalContext.current.findActivity()
    val pdfBuildFailedMessage = stringResource(R.string.upload_pdf_build_failed)
    val apiErrorMessages = rememberApiErrorMessages()

    var documentFile by remember { mutableStateOf<File?>(null) }
    var documentMimeType by remember { mutableStateOf("application/pdf") }
    var pdfProgressCurrent by remember { mutableIntStateOf(0) }
    val totalPages = (pendingUpload as? PendingUpload.Images)?.pages?.size ?: 1

    var uploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var documentText by remember { mutableStateOf<String?>(null) }
    var insights by remember { mutableStateOf(DocumentInsights.EMPTY) }
    var analyzingInsights by remember { mutableStateOf(false) }
    var orgTags by remember { mutableStateOf<List<TagDto>>(emptyList()) }
    var selectedTagIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    /** Vorgeschlagene Tags, die es in der Organisation noch nicht gibt — per Name, nicht vorausgewählt. */
    var selectedNewTagNames by remember { mutableStateOf<Set<String>>(emptySet()) }

    val scope = rememberCoroutineScope()
    // Dekodieren/Rendern der Vorschau läuft im Hintergrund — bei einem
    // hochauflösenden Scan blockiert das sonst spürbar den UI-Thread.
    val previewBitmap by produceState<Bitmap?>(initialValue = null, pendingUpload) {
        value = withContext(Dispatchers.IO) {
            when (pendingUpload) {
                is PendingUpload.Images -> decodeBitmap(pendingUpload.pages.first())
                is PendingUpload.ReadyDocument -> when {
                    pendingUpload.mimeType.startsWith("image/") -> decodeBitmap(pendingUpload.file)
                    pendingUpload.mimeType == "application/pdf" -> renderPdfFirstPage(pendingUpload.file)
                    else -> null
                }
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
                selectedOrg = it.firstOrNull { org -> org.id == store.defaultOrgId } ?: it.firstOrNull()
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

    // Texterkennung bewusst getrennt von der Analyse: die Tag-Liste der Organisation
    // wird nebenläufig geladen und stößt die Analyse ein zweites Mal an — die teure
    // OCR darf dabei nicht erneut über alle Seiten laufen.
    LaunchedEffect(documentFile) {
        val doc = documentFile ?: return@LaunchedEffect
        if (!store.smartInsightsEnabled) return@LaunchedEffect
        analyzingInsights = true
        documentText = when (pendingUpload) {
            is PendingUpload.Images -> if (store.onDeviceOcrEnabled) {
                PdfTextExtractor.extractText(doc)
            } else {
                pdfBuilder.recognizeText(pendingUpload.pages)
            }
            is PendingUpload.ReadyDocument ->
                if (pendingUpload.mimeType == "application/pdf") PdfTextExtractor.extractText(doc) else ""
        }
    }

    LaunchedEffect(documentText, orgTags) {
        val text = documentText ?: return@LaunchedEffect
        analyzingInsights = true
        try {
            val engine = if (store.onDeviceAiEnabled) HybridInsightsEngine else RuleBasedInsightsEngine
            insights = engine.analyze(text, orgTags.map { it.name })
        } finally {
            analyzingInsights = false
        }
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
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
                val preview = previewBitmap
                if (preview != null) {
                    Image(
                        bitmap = preview.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } else if (isReadyDocument) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
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
                                store.defaultOrgId = org.id
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

            val filenameExtension = if (pendingUpload is PendingUpload.Images) {
                "pdf"
            } else {
                extensionForMimeType(documentMimeType)
            }

            // Jeder erkannte Tag-Name wird angezeigt, auch wenn er in der Organisation noch
            // nicht existiert. Bereits vorhandene Tags sind vorausgewählt (siehe LaunchedEffect
            // oben); noch nicht existierende sind nur ein Vorschlag und werden erst beim
            // manuellen Auswählen tatsächlich angelegt (siehe Upload-Button unten).
            val suggestedTagOptions = insights.suggestedTagNames.map { name ->
                orgTags.firstOrNull { it.name.equals(name, ignoreCase = true) }
                    ?.let { SuggestedTagOption.Existing(it) }
                    ?: SuggestedTagOption.New(name)
            }
            val hasSuggestions = insights.documentDate != null ||
                insights.senderName != null ||
                suggestedTagOptions.isNotEmpty()

            if (store.smartInsightsEnabled && (analyzingInsights || hasSuggestions)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (store.onDeviceAiEnabled) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.height(16.dp),
                            )
                        }
                        Text(
                            text = stringResource(R.string.upload_smart_suggestions_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (analyzingInsights) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.height(16.dp).width(16.dp),
                            )
                            Text(
                                text = stringResource(R.string.upload_analyzing_document),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        insights.documentDate?.let { docDate ->
                            AssistChip(
                                onClick = {
                                    fileName = FilenameTemplate.render(
                                        template = store.filenameTemplate,
                                        organizationName = selectedOrg?.name,
                                        counter = store.uploadCount + 1,
                                        documentDate = docDate,
                                    ) + ".$filenameExtension"
                                },
                                label = { Text(stringResource(R.string.upload_use_document_date, docDate.format(DISPLAY_DATE_FORMATTER))) },
                            )
                        }

                        insights.senderName?.let { sender ->
                            AssistChip(
                                onClick = {
                                    fileName = FilenameTemplate.render(
                                        template = store.filenameTemplate,
                                        organizationName = selectedOrg?.name,
                                        counter = store.uploadCount + 1,
                                        senderName = sender,
                                    ) + ".$filenameExtension"
                                },
                                label = { Text(stringResource(R.string.upload_use_sender_name, sender)) },
                            )
                        }

                        if (suggestedTagOptions.isNotEmpty()) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                suggestedTagOptions.forEach { option ->
                                    val selected = when (option) {
                                        is SuggestedTagOption.Existing -> option.tag.id in selectedTagIds
                                        is SuggestedTagOption.New -> option.name in selectedNewTagNames
                                    }
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            when (option) {
                                                is SuggestedTagOption.Existing -> {
                                                    selectedTagIds = if (selected) {
                                                        selectedTagIds - option.tag.id
                                                    } else {
                                                        selectedTagIds + option.tag.id
                                                    }
                                                }
                                                is SuggestedTagOption.New -> {
                                                    selectedNewTagNames = if (selected) {
                                                        selectedNewTagNames - option.name
                                                    } else {
                                                        selectedNewTagNames + option.name
                                                    }
                                                }
                                            }
                                        },
                                        label = { Text(option.name) },
                                    )
                                }
                            }
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

                        if (selectedTagIds.isNotEmpty() || selectedNewTagNames.isNotEmpty()) {
                            withContext(Dispatchers.IO) {
                                selectedTagIds.forEach { tagId ->
                                    runCatching { client.addTagToDocument(org.id, uploadedDoc.id, tagId) }
                                }
                                // Manuell ausgewählte, noch nicht existierende Vorschläge werden
                                // jetzt erst angelegt — nicht schon bei der bloßen Anzeige des Chips.
                                selectedNewTagNames.forEach { name ->
                                    client.createTag(organizationId = org.id, name = name, color = NEW_TAG_COLOR)
                                        .onSuccess { newTag ->
                                            runCatching { client.addTagToDocument(org.id, uploadedDoc.id, newTag.id) }
                                        }
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

                        if (shouldPromptSupport) {
                            showSupportDialog = true
                        } else {
                            // Tut nichts, solange die Bewertung nicht fällig ist, und
                            // wartet sonst, bis der Play-Dialog geschlossen wurde.
                            activity?.let { reviewPrompt.requestIfDue(it) }
                            onDone()
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

private fun decodeBitmap(file: File): Bitmap? =
    runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()

private val DISPLAY_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.US)

private fun ensureExtension(name: String, mimeType: String): String {
    if (name.contains('.')) return name
    return "$name.${extensionForMimeType(mimeType)}"
}

/** Ein vorgeschlagener Tag: entweder schon in der Organisation vorhanden oder (noch) nicht. */
private sealed interface SuggestedTagOption {
    val name: String

    data class Existing(val tag: TagDto) : SuggestedTagOption {
        override val name get() = tag.name
    }

    data class New(override val name: String) : SuggestedTagOption
}

/** Default-Farbe für automatisch angelegte Smart-Suggestion-Tags — gleicher Wert wie TagsScreens Standard-Swatch. */
private const val NEW_TAG_COLOR = "#D8FF75"
