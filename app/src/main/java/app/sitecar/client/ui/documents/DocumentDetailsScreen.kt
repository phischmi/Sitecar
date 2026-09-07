package app.sitecar.client.ui.documents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.sitecar.client.R
import app.sitecar.client.data.DocumentActivityDto
import app.sitecar.client.data.DocumentDetailDto
import app.sitecar.client.data.SitecarApiClient
import app.sitecar.client.data.TagDto
import app.sitecar.client.ui.util.friendlyErrorMessage
import app.sitecar.client.ui.util.rememberApiErrorMessages
import kotlinx.coroutines.launch
import java.time.Instant

private enum class DetailsTab { INFO, CONTENT, ACTIVITY }

/**
 * Detailansicht eines Dokuments mit denselben drei Reitern wie die Papra-Weboberfläche:
 * Info (Metadaten, Dokumentdatum und Notiz), Inhalt (der extrahierte Text) und
 * Aktivität (die Änderungshistorie). Tags, Notiz, Inhalt und Dokumentdatum lassen
 * sich hier ändern; jede Änderung geht einzeln an den Server und erscheint danach
 * in der Historie.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailsScreen(
    client: SitecarApiClient,
    organizationId: String,
    documentId: String,
    initialName: String?,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val apiErrorMessages = rememberApiErrorMessages()

    var document by remember { mutableStateOf<DocumentDetailDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(DetailsTab.INFO) }

    var notes by remember { mutableStateOf("") }
    var notesSaving by remember { mutableStateOf(false) }
    var notesSaved by remember { mutableStateOf(false) }

    var content by remember { mutableStateOf("") }
    var contentSaving by remember { mutableStateOf(false) }
    var contentSaved by remember { mutableStateOf(false) }

    var dateSaving by remember { mutableStateOf(false) }
    var datePickerOpen by remember { mutableStateOf(false) }

    var orgTags by remember { mutableStateOf<List<TagDto>>(emptyList()) }
    var orgTagsLoaded by remember { mutableStateOf(false) }
    // Während eine Zuweisung läuft, sind alle Tag-Pillen gesperrt: der Zustand des
    // Dokuments steht erst mit der Antwort fest.
    var togglingTag by remember { mutableStateOf(false) }

    var activities by remember { mutableStateOf<List<DocumentActivityDto>?>(null) }
    var activityLoading by remember { mutableStateOf(false) }
    var activityError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(organizationId, documentId) {
        loading = true
        client.getDocument(organizationId = organizationId, documentId = documentId)
            .onSuccess {
                document = it
                notes = it.notes.orEmpty()
                content = it.content.orEmpty()
            }
            .onFailure { errorMessage = friendlyErrorMessage(it, apiErrorMessages) }
        loading = false
        // Für das Zuweisen von Tags braucht es alle Tags der Organisation; schlägt
        // das fehl, bleiben unten nur die bereits gesetzten Tags stehen.
        client.listTags(organizationId).onSuccess {
            orgTags = it
            orgTagsLoaded = true
        }
    }

    // Die Historie wird erst beim ersten Öffnen des Reiters geholt; sie ist eine
    // eigene Anfrage, die für Info und Inhalt niemand braucht.
    LaunchedEffect(selectedTab) {
        if (selectedTab != DetailsTab.ACTIVITY || activities != null || activityLoading) return@LaunchedEffect
        activityLoading = true
        activityError = null
        client.listDocumentActivity(organizationId = organizationId, documentId = documentId)
            .onSuccess { activities = it }
            .onFailure { activityError = friendlyErrorMessage(it, apiErrorMessages) }
        activityLoading = false
    }

    fun saveNotes() {
        notesSaving = true
        notesSaved = false
        errorMessage = null
        scope.launch {
            client.updateDocumentNotes(
                organizationId = organizationId,
                documentId = documentId,
                notes = notes,
            ).onSuccess {
                document = document?.copy(notes = it.notes, updatedAt = it.updatedAt)
                notesSaved = true
                // Die Änderung taucht als neuer Eintrag in der Historie auf.
                activities = null
            }.onFailure { errorMessage = friendlyErrorMessage(it, apiErrorMessages) }
            notesSaving = false
        }
    }

    fun saveContent() {
        contentSaving = true
        contentSaved = false
        errorMessage = null
        scope.launch {
            client.updateDocumentContent(
                organizationId = organizationId,
                documentId = documentId,
                content = content,
            ).onSuccess {
                document = document?.copy(content = it.content, updatedAt = it.updatedAt)
                contentSaved = true
                activities = null
            }.onFailure { errorMessage = friendlyErrorMessage(it, apiErrorMessages) }
            contentSaving = false
        }
    }

    fun toggleTag(tag: TagDto) {
        val doc = document ?: return
        val assigned = doc.tags.any { it.id == tag.id }
        togglingTag = true
        errorMessage = null
        scope.launch {
            val result = if (assigned) {
                client.removeTagFromDocument(organizationId, documentId, tag.id)
            } else {
                client.addTagToDocument(organizationId, documentId, tag.id)
            }
            result.onSuccess {
                val tags = if (assigned) doc.tags.filterNot { it.id == tag.id } else doc.tags + tag
                document = document?.copy(tags = tags)
                activities = null
            }.onFailure { errorMessage = friendlyErrorMessage(it, apiErrorMessages) }
            togglingTag = false
        }
    }

    /** [isoDate] = null löscht das Dokumentdatum. */
    fun saveDocumentDate(isoDate: String?) {
        dateSaving = true
        errorMessage = null
        scope.launch {
            client.updateDocumentDate(
                organizationId = organizationId,
                documentId = documentId,
                documentDate = isoDate,
            ).onSuccess {
                document = document?.copy(documentDate = it.documentDate, updatedAt = it.updatedAt)
                activities = null
            }.onFailure { errorMessage = friendlyErrorMessage(it, apiErrorMessages) }
            dateSaving = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = document?.name ?: initialName ?: stringResource(R.string.document_details_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
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
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
            PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                DetailsTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(stringResource(tabLabel(tab))) },
                    )
                }
            }

            val doc = document
            when {
                loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                doc == null -> ListStateMessage(text = errorMessage ?: stringResource(R.string.documents_empty))
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    errorMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    when (selectedTab) {
                        DetailsTab.INFO -> InfoTab(
                            doc = doc,
                            notes = notes,
                            onNotesChange = {
                                notes = it
                                notesSaved = false
                            },
                            notesSaving = notesSaving,
                            notesSaved = notesSaved,
                            onSaveNotes = { saveNotes() },
                            dateSaving = dateSaving,
                            onEditDate = { datePickerOpen = true },
                            orgTags = orgTags,
                            orgTagsLoaded = orgTagsLoaded,
                            togglingTag = togglingTag,
                            onToggleTag = { toggleTag(it) },
                        )
                        DetailsTab.CONTENT -> EditableTextSection(
                            value = content,
                            onValueChange = {
                                content = it
                                contentSaved = false
                            },
                            placeholder = stringResource(R.string.document_details_content_placeholder),
                            minLines = 8,
                            saveLabel = stringResource(R.string.document_details_content_save),
                            savedLabel = stringResource(R.string.document_details_content_saved),
                            saving = contentSaving,
                            saved = contentSaved,
                            onSave = { saveContent() },
                        )
                        DetailsTab.ACTIVITY -> ActivityTab(
                            activities = activities,
                            loading = activityLoading,
                            error = activityError,
                        )
                    }
                }
            }
        }
    }

    if (datePickerOpen) {
        DocumentDatePickerDialog(
            currentDate = document?.documentDate,
            onDismiss = { datePickerOpen = false },
            onPicked = { isoDate ->
                datePickerOpen = false
                saveDocumentDate(isoDate)
            },
        )
    }
}

/**
 * Datumsauswahl für das Dokumentdatum. Der Dialog arbeitet wie die
 * Weboberfläche mit UTC-Mitternacht des gewählten Tages und kann das Datum über
 * "Datum entfernen" auch ganz löschen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentDatePickerDialog(
    currentDate: String?,
    onDismiss: () -> Unit,
    onPicked: (String?) -> Unit,
) {
    val initialMillis = currentDate
        ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onPicked(state.selectedDateMillis?.let { Instant.ofEpochMilli(it).toString() })
                },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onPicked(null) }) {
                    Text(stringResource(R.string.document_details_date_clear))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        },
    ) {
        DatePicker(state = state, title = { Text(stringResource(R.string.document_details_date_title)) })
    }
}

/** Mehrzeiliges Feld mit Speichern-Button — für Notiz und extrahierten Text. */
@Composable
private fun EditableTextSection(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int,
    saveLabel: String,
    savedLabel: String,
    saving: Boolean,
    saved: Boolean,
    onSave: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        minLines = minLines,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onSave, enabled = !saving) {
            if (saving) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Text(saveLabel)
            }
        }
        if (saved) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = savedLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoTab(
    doc: DocumentDetailDto,
    notes: String,
    onNotesChange: (String) -> Unit,
    notesSaving: Boolean,
    notesSaved: Boolean,
    onSaveNotes: () -> Unit,
    dateSaving: Boolean,
    onEditDate: () -> Unit,
    orgTags: List<TagDto>,
    orgTagsLoaded: Boolean,
    togglingTag: Boolean,
    onToggleTag: (TagDto) -> Unit,
) {
    InfoRow(stringResource(R.string.document_details_info_name), doc.name ?: doc.id)
    InfoRow(stringResource(R.string.document_details_info_type), doc.mimeType.orEmpty())
    InfoRow(stringResource(R.string.document_details_info_size), formatSize(doc.originalSize))
    InfoRow(
        label = stringResource(R.string.document_details_info_document_date),
        value = doc.documentDate?.let { formatDate(it) }?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.document_details_info_no_date),
        onClick = onEditDate.takeIf { !dateSaving },
        busy = dateSaving,
    )
    InfoRow(stringResource(R.string.document_details_info_created_at), formatDateTime(doc.createdAt))
    InfoRow(
        label = stringResource(R.string.document_details_info_updated_at),
        value = formatDateTime(doc.updatedAt).takeIf { it.isNotBlank() }
            ?: stringResource(R.string.document_details_info_never),
    )
    InfoRow(stringResource(R.string.document_details_info_id), doc.id)

    // Alle Tags der Organisation als Pillen: die gesetzten ausgefüllt, die übrigen
    // nur umrandet — ein Tippen weist zu bzw. entfernt. Ließen sich die Tags der
    // Organisation nicht laden, bleiben wenigstens die gesetzten sichtbar.
    val tags = orgTags.ifEmpty { doc.tags }
    if (tags.isNotEmpty() || orgTagsLoaded) {
        Text(
            text = stringResource(R.string.document_details_info_tags),
            style = MaterialTheme.typography.labelLarge,
        )
        if (tags.isEmpty()) {
            Text(
                text = stringResource(R.string.documents_manage_tags_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                tags.forEach { tag ->
                    TagPill(
                        name = tag.name,
                        colorHex = tag.color,
                        onClick = { if (!togglingTag) onToggleTag(tag) },
                        selected = doc.tags.any { it.id == tag.id },
                    )
                }
            }
        }
    }

    HorizontalDivider()

    Text(
        text = stringResource(R.string.document_details_notes_label),
        style = MaterialTheme.typography.labelLarge,
    )
    EditableTextSection(
        value = notes,
        onValueChange = onNotesChange,
        placeholder = stringResource(R.string.document_details_notes_placeholder),
        minLines = 3,
        saveLabel = stringResource(R.string.document_details_notes_save),
        savedLabel = stringResource(R.string.document_details_notes_saved),
        saving = notesSaving,
        saved = notesSaved,
        onSave = onSaveNotes,
    )
}

@Composable
private fun ActivityTab(
    activities: List<DocumentActivityDto>?,
    loading: Boolean,
    error: String?,
) {
    when {
        loading -> CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        error != null -> Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        activities.isNullOrEmpty() -> Text(
            text = stringResource(R.string.document_details_activity_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        else -> activities.forEach { activity ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = activityIcon(activity.event),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Column {
                    Text(text = activityText(activity), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = listOfNotNull(
                            formatDateTime(activity.createdAt).takeIf { it.isNotBlank() },
                            activity.user?.name?.takeIf { it.isNotBlank() }
                                ?.let { stringResource(R.string.document_details_activity_by, it) },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Eine Label-Wert-Zeile; mit [onClick] wird der Wert bearbeitbar und bekommt ein Stift-Symbol. */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
    busy: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (busy) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private fun tabLabel(tab: DetailsTab): Int = when (tab) {
    DetailsTab.INFO -> R.string.document_details_tab_info
    DetailsTab.CONTENT -> R.string.document_details_tab_content
    DetailsTab.ACTIVITY -> R.string.document_details_tab_activity
}

private fun activityIcon(event: String): ImageVector = when (event) {
    "created" -> Icons.AutoMirrored.Filled.NoteAdd
    "updated" -> Icons.Default.Edit
    "deleted" -> Icons.Default.Delete
    "restored" -> Icons.Default.RestoreFromTrash
    "tagged", "untagged" -> Icons.Default.Sell
    else -> Icons.Default.HistoryToggleOff
}

/**
 * Der Server nennt in der Historie die rohen API-Feldnamen; bekannte werden
 * übersetzt, unbekannte bleiben unverändert stehen.
 */
@Composable
private fun activityFieldLabel(field: String): String = when (field) {
    "name" -> stringResource(R.string.document_details_field_name)
    "content" -> stringResource(R.string.document_details_field_content)
    "documentDate" -> stringResource(R.string.document_details_field_document_date)
    "notes" -> stringResource(R.string.document_details_field_notes)
    else -> field
}

@Composable
private fun activityText(activity: DocumentActivityDto): String {
    val tagName = activity.tag?.name.orEmpty()
    return when (activity.event) {
        "created" -> stringResource(R.string.document_details_activity_created)
        "deleted" -> stringResource(R.string.document_details_activity_deleted)
        "restored" -> stringResource(R.string.document_details_activity_restored)
        "tagged" -> stringResource(R.string.document_details_activity_tagged, tagName)
        "untagged" -> stringResource(R.string.document_details_activity_untagged, tagName)
        "updated" -> {
            val fields = activity.eventData?.updatedFields.orEmpty().map { activityFieldLabel(it) }
            when {
                fields.isEmpty() -> stringResource(R.string.document_details_activity_updated)
                fields.size == 1 -> stringResource(R.string.document_details_activity_updated_field, fields.first())
                else -> stringResource(
                    R.string.document_details_activity_updated_fields,
                    fields.joinToString(", "),
                )
            }
        }
        else -> activity.event
    }
}
