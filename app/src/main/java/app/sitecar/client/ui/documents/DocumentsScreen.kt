package app.sitecar.client.ui.documents

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.sitecar.client.R
import app.sitecar.client.data.DocumentDto
import app.sitecar.client.data.DocumentSortField
import app.sitecar.client.data.DocumentSortOrder
import app.sitecar.client.data.DocumentThumbnailLoader
import app.sitecar.client.data.Organization
import app.sitecar.client.data.SitecarApiClient
import app.sitecar.client.data.SettingsStore
import app.sitecar.client.data.SwipeAction
import app.sitecar.client.data.TagDto
import app.sitecar.client.ui.util.friendlyErrorMessage
import app.sitecar.client.ui.util.rememberApiErrorMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    client: SitecarApiClient,
    store: SettingsStore,
    onOpenSettings: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    initialSearchQuery: String? = null,
    onConsumeInitialSearchQuery: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val thumbnailLoader = remember(client) {
        DocumentThumbnailLoader(client, File(context.cacheDir, "thumbnails"))
    }

    var orgs by remember { mutableStateOf<List<Organization>>(emptyList()) }
    var selectedOrg by remember { mutableStateOf<Organization?>(null) }

    var documents by remember { mutableStateOf<List<DocumentDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var openingDocumentId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf(initialSearchQuery.orEmpty()) }
    var sortField by remember { mutableStateOf(DocumentSortField.CREATED_AT) }
    var sortOrder by remember { mutableStateOf(DocumentSortOrder.DESC) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var menuOpenForId by remember { mutableStateOf<String?>(null) }
    var docPendingDelete by remember { mutableStateOf<DocumentDto?>(null) }
    var deletingDocumentId by remember { mutableStateOf<String?>(null) }
    var docPendingRename by remember { mutableStateOf<DocumentDto?>(null) }
    var renameText by remember { mutableStateOf("") }
    var renamingDocumentId by remember { mutableStateOf<String?>(null) }
    var docPendingTags by remember { mutableStateOf<DocumentDto?>(null) }
    var orgTags by remember { mutableStateOf<List<TagDto>>(emptyList()) }
    var orgTagsLoading by remember { mutableStateOf(false) }
    var togglingTagId by remember { mutableStateOf<String?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedDocIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var bulkTagsDialogOpen by remember { mutableStateOf(false) }
    var bulkDeleteDialogOpen by remember { mutableStateOf(false) }
    var bulkActionInProgress by remember { mutableStateOf(false) }
    val swipeStartToEndAction by store.swipeStartToEndActionFlow.collectAsState(initial = store.swipeStartToEndAction)
    val swipeEndToStartAction by store.swipeEndToStartActionFlow.collectAsState(initial = store.swipeEndToStartAction)
    val apiErrorMessages = rememberApiErrorMessages()
    val bulkDeletePartialFailureMessage = stringResource(R.string.documents_bulk_delete_partial_failure)
    val bulkTagsPartialFailureMessage = stringResource(R.string.documents_bulk_tags_partial_failure)

    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedDocIds = emptySet()
    }

    suspend fun fetchDocuments(org: Organization, query: String) {
        loading = true
        errorMessage = null
        client.listDocuments(
            organizationId = org.id,
            searchQuery = query.takeIf { it.isNotBlank() },
            sortField = sortField,
            sortOrder = sortOrder,
        )
            .onSuccess { documents = it.documents }
            .onFailure { errorMessage = friendlyErrorMessage(it, apiErrorMessages) }
        loading = false
    }

    LaunchedEffect(Unit) {
        if (!initialSearchQuery.isNullOrBlank()) {
            onConsumeInitialSearchQuery()
        }
        client.listOrganizations()
            .onSuccess { list ->
                orgs = list
                selectedOrg = list.firstOrNull { it.id == store.defaultOrgId } ?: list.firstOrNull()
            }
            .onFailure { errorMessage = friendlyErrorMessage(it, apiErrorMessages) }
    }

    LaunchedEffect(selectedOrg, sortField, sortOrder) {
        val org = selectedOrg ?: return@LaunchedEffect
        fetchDocuments(org, searchQuery)
    }

    LaunchedEffect(searchQuery) {
        val org = selectedOrg ?: return@LaunchedEffect
        delay(350)
        fetchDocuments(org, searchQuery)
    }

    fun deleteDocument(doc: DocumentDto) {
        val org = selectedOrg ?: return
        deletingDocumentId = doc.id
        scope.launch {
            client.deleteDocument(organizationId = org.id, documentId = doc.id)
                .onSuccess { documents = documents.filterNot { it.id == doc.id } }
                .onFailure { errorMessage = friendlyErrorMessage(it, apiErrorMessages) }
            deletingDocumentId = null
        }
    }

    fun renameDocument(doc: DocumentDto, newName: String) {
        val org = selectedOrg ?: return
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == doc.name) return
        renamingDocumentId = doc.id
        scope.launch {
            client.updateDocumentName(organizationId = org.id, documentId = doc.id, name = trimmed)
                .onSuccess { updated -> documents = documents.map { if (it.id == doc.id) updated else it } }
                .onFailure { errorMessage = friendlyErrorMessage(it, apiErrorMessages) }
            renamingDocumentId = null
        }
    }

    fun ensureOrgTagsLoaded(org: Organization) {
        if (orgTags.isNotEmpty() || orgTagsLoading) return
        orgTagsLoading = true
        scope.launch {
            client.listTags(organizationId = org.id)
                .onSuccess { orgTags = it }
                .onFailure { errorMessage = friendlyErrorMessage(it, apiErrorMessages) }
            orgTagsLoading = false
        }
    }

    fun clearSelection() {
        selectionMode = false
        selectedDocIds = emptySet()
    }

    fun startSelection(id: String) {
        selectionMode = true
        selectedDocIds = setOf(id)
        menuOpenForId = null
    }

    fun toggleSelection(id: String) {
        selectedDocIds = if (id in selectedDocIds) selectedDocIds - id else selectedDocIds + id
        if (selectedDocIds.isEmpty()) selectionMode = false
    }

    fun bulkDelete() {
        val org = selectedOrg ?: return
        val ids = selectedDocIds
        bulkActionInProgress = true
        scope.launch {
            val succeeded = mutableSetOf<String>()
            ids.forEach { id ->
                client.deleteDocument(organizationId = org.id, documentId = id).onSuccess { succeeded += id }
            }
            documents = documents.filterNot { it.id in succeeded }
            bulkActionInProgress = false
            if (succeeded.size < ids.size) {
                errorMessage = bulkDeletePartialFailureMessage
            }
            clearSelection()
        }
    }

    // Setzt/entfernt einen Tag bei allen ausgewählten Dokumenten: "checked" bedeutet
    // "haben ihn schon alle" — ein Klick schaltet dann für alle in die jeweils andere
    // Richtung, Dokumente, die den Zieltag schon/noch nicht hatten, bleiben unberührt.
    fun toggleBulkTag(tag: TagDto, currentlyAllHave: Boolean) {
        val org = selectedOrg ?: return
        val targets = documents.filter { it.id in selectedDocIds }
        togglingTagId = tag.id
        scope.launch {
            var anyFailure = false
            targets.forEach { doc ->
                val hasTag = doc.tags.any { it.id == tag.id }
                val result = when {
                    currentlyAllHave && hasTag ->
                        client.removeTagFromDocument(organizationId = org.id, documentId = doc.id, tagId = tag.id)
                    !currentlyAllHave && !hasTag ->
                        client.addTagToDocument(organizationId = org.id, documentId = doc.id, tagId = tag.id)
                    else -> Result.success(Unit)
                }
                if (result.isFailure) anyFailure = true
            }
            documents = documents.map { doc ->
                if (doc.id in selectedDocIds) {
                    val updatedTags = if (currentlyAllHave) {
                        doc.tags.filterNot { it.id == tag.id }
                    } else {
                        (doc.tags + tag).distinctBy { it.id }
                    }
                    doc.copy(tags = updatedTags)
                } else {
                    doc
                }
            }
            if (anyFailure) errorMessage = bulkTagsPartialFailureMessage
            togglingTagId = null
        }
    }

    // Wischgesten lösen dieselben Aktionen aus wie das "..."-Kontextmenü — nie
    // direkt destruktiv, sondern öffnen den jeweiligen Bestätigungs-/Bearbeiten-
    // Dialog, damit ein versehentliches Wischen kein Dokument sofort löscht.
    fun performSwipeAction(action: SwipeAction, doc: DocumentDto) {
        when (action) {
            SwipeAction.DELETE -> docPendingDelete = doc
            SwipeAction.RENAME -> {
                renameText = doc.name.orEmpty()
                docPendingRename = doc
            }
            SwipeAction.EDIT_TAGS -> {
                docPendingTags = doc
                selectedOrg?.let { ensureOrgTagsLoaded(it) }
            }
            SwipeAction.NONE -> {}
        }
    }

    fun toggleTag(doc: DocumentDto, tag: TagDto) {
        val org = selectedOrg ?: return
        val isAssigned = doc.tags.any { it.id == tag.id }
        togglingTagId = tag.id
        scope.launch {
            val result = if (isAssigned) {
                client.removeTagFromDocument(organizationId = org.id, documentId = doc.id, tagId = tag.id)
            } else {
                client.addTagToDocument(organizationId = org.id, documentId = doc.id, tagId = tag.id)
            }
            result.onSuccess {
                val updatedTags = if (isAssigned) doc.tags.filterNot { it.id == tag.id } else doc.tags + tag
                documents = documents.map { if (it.id == doc.id) it.copy(tags = updatedTags) else it }
                docPendingTags = docPendingTags?.takeIf { it.id == doc.id }?.copy(tags = updatedTags) ?: docPendingTags
            }.onFailure { errorMessage = friendlyErrorMessage(it, apiErrorMessages) }
            togglingTagId = null
        }
    }

    fun openDocument(doc: DocumentDto) {
        val org = selectedOrg ?: return
        if (openingDocumentId != null) return
        openingDocumentId = doc.id
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                client.downloadDocumentFile(organizationId = org.id, documentId = doc.id)
                    .mapCatching { bytes ->
                        val dir = File(context.cacheDir, "documents").apply { mkdirs() }
                        val file = File(dir, "${doc.id}-${doc.name ?: doc.id}")
                        file.writeBytes(bytes)
                        file
                    }
            }
            openingDocumentId = null
            result.onSuccess { file ->
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, doc.mimeType ?: "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runCatching { context.startActivity(intent) }
                    .onFailure {
                        errorMessage = if (it is ActivityNotFoundException) {
                            context.getString(R.string.documents_no_viewer)
                        } else {
                            friendlyErrorMessage(it, apiErrorMessages)
                        }
                    }
            }.onFailure { errorMessage = friendlyErrorMessage(it, apiErrorMessages) }
        }
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = { Text(stringResource(R.string.documents_selected_count, selectedDocIds.size)) },
                    navigationIcon = {
                        IconButton(onClick = { clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                        }
                    },
                    actions = {
                        if (bulkActionInProgress) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp).padding(horizontal = 12.dp),
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    bulkTagsDialogOpen = true
                                    selectedOrg?.let { ensureOrgTagsLoaded(it) }
                                },
                            ) {
                                Icon(Icons.Default.Sell, contentDescription = stringResource(R.string.action_manage_tags))
                            }
                            IconButton(onClick = { bulkDeleteDialogOpen = true }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                            }
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.documents_title)) },
                    actions = {
                        Box {
                            IconButton(onClick = { sortMenuOpen = true }) {
                                Icon(Icons.Default.Sort, contentDescription = stringResource(R.string.documents_sort))
                            }
                            DropdownMenu(
                                expanded = sortMenuOpen,
                                onDismissRequest = { sortMenuOpen = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.documents_sort_date_desc)) },
                                    onClick = {
                                        sortField = DocumentSortField.CREATED_AT
                                        sortOrder = DocumentSortOrder.DESC
                                        sortMenuOpen = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.documents_sort_date_asc)) },
                                    onClick = {
                                        sortField = DocumentSortField.CREATED_AT
                                        sortOrder = DocumentSortOrder.ASC
                                        sortMenuOpen = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.documents_sort_name_asc)) },
                                    onClick = {
                                        sortField = DocumentSortField.NAME
                                        sortOrder = DocumentSortOrder.ASC
                                        sortMenuOpen = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.documents_sort_name_desc)) },
                                    onClick = {
                                        sortField = DocumentSortField.NAME
                                        sortOrder = DocumentSortOrder.DESC
                                        sortMenuOpen = false
                                    },
                                )
                            }
                        }
                        OrgSwitcherAction(
                            orgs = orgs,
                            selectedOrg = selectedOrg,
                            onSelect = { org ->
                                selectedOrg = org
                                store.defaultOrgId = org.id
                            },
                        )
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings))
                        }
                    },
                )
            }
        },
        bottomBar = bottomBar,
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.documents_search_hint)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_clear_search))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp),
            )

            PullToRefreshBox(
                isRefreshing = loading,
                onRefresh = {
                    val org = selectedOrg
                    if (org != null) {
                        scope.launch { fetchDocuments(org, searchQuery) }
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                when {
                    errorMessage != null -> Box(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.documents_load_failed, errorMessage.orEmpty()),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                    documents.isEmpty() && !loading -> Box(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                stringResource(R.string.documents_search_empty)
                            } else {
                                stringResource(R.string.documents_empty)
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    documents.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        val orgId = selectedOrg?.id.orEmpty()
                        items(documents, key = { it.id }) { doc ->
                            val rowContent: @Composable () -> Unit = {
                                DocumentRow(
                                    doc = doc,
                                    organizationId = orgId,
                                    thumbnailLoader = thumbnailLoader,
                                    showOpeningIndicator = openingDocumentId == doc.id,
                                    clickEnabled = openingDocumentId == null,
                                    isBusy = deletingDocumentId == doc.id || renamingDocumentId == doc.id,
                                    menuExpanded = menuOpenForId == doc.id,
                                    onMenuExpandedChange = { expanded -> menuOpenForId = if (expanded) doc.id else null },
                                    selectionMode = selectionMode,
                                    selected = doc.id in selectedDocIds,
                                    onClick = {
                                        if (selectionMode) toggleSelection(doc.id) else openDocument(doc)
                                    },
                                    onLongClick = {
                                        if (selectionMode) toggleSelection(doc.id) else startSelection(doc.id)
                                    },
                                    onRename = {
                                        renameText = doc.name.orEmpty()
                                        docPendingRename = doc
                                    },
                                    onManageTags = {
                                        docPendingTags = doc
                                        selectedOrg?.let { ensureOrgTagsLoaded(it) }
                                    },
                                    onDelete = { docPendingDelete = doc },
                                    onTagClick = { tag -> searchQuery = buildTagSearchQuery(tag.name) },
                                )
                            }

                            val swipeEnabled = !selectionMode &&
                                (swipeStartToEndAction != SwipeAction.NONE || swipeEndToStartAction != SwipeAction.NONE)
                            if (!swipeEnabled) {
                                rowContent()
                            } else {
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        val action = when (value) {
                                            SwipeToDismissBoxValue.StartToEnd -> swipeStartToEndAction
                                            SwipeToDismissBoxValue.EndToStart -> swipeEndToStartAction
                                            SwipeToDismissBoxValue.Settled -> SwipeAction.NONE
                                        }
                                        performSwipeAction(action, doc)
                                        // Nie wirklich "dismissen" — die Aktion öffnet nur den
                                        // passenden Dialog, das Listenelement bleibt bestehen.
                                        false
                                    },
                                    // Deutlich höher als das Default (50 %), damit ein schneller
                                    // vertikaler Scroll-Fling mit leichtem horizontalem Versatz
                                    // nicht versehentlich als Wisch-Aktion durchgeht — erst ein
                                    // bewusst weiter geführter Wisch löst die Aktion aus.
                                    positionalThreshold = { totalDistance -> totalDistance * 0.75f },
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = swipeStartToEndAction != SwipeAction.NONE,
                                    enableDismissFromEndToStart = swipeEndToStartAction != SwipeAction.NONE,
                                    backgroundContent = {
                                        SwipeActionBackground(
                                            direction = dismissState.dismissDirection,
                                            startAction = swipeStartToEndAction,
                                            endAction = swipeEndToStartAction,
                                        )
                                    },
                                ) {
                                    rowContent()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val deleteTarget = docPendingDelete
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { docPendingDelete = null },
            title = { Text(stringResource(R.string.documents_delete_title)) },
            text = { Text(stringResource(R.string.documents_delete_message, deleteTarget.name ?: deleteTarget.id)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteDocument(deleteTarget)
                        docPendingDelete = null
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { docPendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    val renameTarget = docPendingRename
    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { docPendingRename = null },
            title = { Text(stringResource(R.string.documents_rename_title)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.upload_filename)) },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = renameText.isNotBlank(),
                    onClick = {
                        renameDocument(renameTarget, renameText)
                        docPendingRename = null
                    },
                ) {
                    Text(stringResource(R.string.settings_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { docPendingRename = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    val tagsTarget = docPendingTags
    if (tagsTarget != null) {
        AlertDialog(
            onDismissRequest = { docPendingTags = null },
            title = { Text(stringResource(R.string.documents_manage_tags_title)) },
            text = {
                when {
                    orgTagsLoading -> Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                    orgTags.isEmpty() -> Text(stringResource(R.string.documents_manage_tags_empty))
                    else -> Column(
                        modifier = Modifier
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        orgTags.forEach { tag ->
                            val checked = tagsTarget.tags.any { it.id == tag.id }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = togglingTagId != tag.id) { toggleTag(tagsTarget, tag) }
                                    .padding(vertical = 4.dp),
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { toggleTag(tagsTarget, tag) },
                                    enabled = togglingTagId != tag.id,
                                )
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(tag.color))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                                )
                                Text(tag.name, modifier = Modifier.padding(start = 8.dp).weight(1f))
                                if (togglingTagId == tag.id) {
                                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { docPendingTags = null }) {
                    Text(stringResource(R.string.action_done))
                }
            },
        )
    }

    if (bulkDeleteDialogOpen) {
        AlertDialog(
            onDismissRequest = { bulkDeleteDialogOpen = false },
            title = { Text(stringResource(R.string.documents_bulk_delete_title, selectedDocIds.size)) },
            text = { Text(stringResource(R.string.documents_bulk_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        bulkDeleteDialogOpen = false
                        bulkDelete()
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { bulkDeleteDialogOpen = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (bulkTagsDialogOpen) {
        val bulkTargets = documents.filter { it.id in selectedDocIds }
        AlertDialog(
            onDismissRequest = { bulkTagsDialogOpen = false },
            title = { Text(stringResource(R.string.documents_bulk_tags_title, selectedDocIds.size)) },
            text = {
                when {
                    orgTagsLoading -> Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                    orgTags.isEmpty() -> Text(stringResource(R.string.documents_manage_tags_empty))
                    else -> Column(
                        modifier = Modifier
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        orgTags.forEach { tag ->
                            val allHave = bulkTargets.isNotEmpty() && bulkTargets.all { it.tags.any { t -> t.id == tag.id } }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = togglingTagId != tag.id) { toggleBulkTag(tag, allHave) }
                                    .padding(vertical = 4.dp),
                            ) {
                                Checkbox(
                                    checked = allHave,
                                    onCheckedChange = { toggleBulkTag(tag, allHave) },
                                    enabled = togglingTagId != tag.id,
                                )
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(tag.color))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                                )
                                Text(tag.name, modifier = Modifier.padding(start = 8.dp).weight(1f))
                                if (togglingTagId == tag.id) {
                                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { bulkTagsDialogOpen = false }) {
                    Text(stringResource(R.string.action_done))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun DocumentRow(
    doc: DocumentDto,
    organizationId: String,
    thumbnailLoader: DocumentThumbnailLoader,
    showOpeningIndicator: Boolean,
    clickEnabled: Boolean,
    isBusy: Boolean,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: () -> Unit,
    onManageTags: () -> Unit,
    onDelete: () -> Unit,
    onTagClick: (TagDto) -> Unit,
) {
    ListItem(
        headlineContent = { Text(doc.name ?: doc.id) },
        supportingContent = {
            Column {
                Text("${formatDate(doc.createdAt)} · ${formatSize(doc.originalSize)}")
                if (doc.tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        doc.tags.forEach { tag ->
                            TagChip(tag = tag, onClick = { onTagClick(tag) })
                        }
                    }
                }
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (showOpeningIndicator) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    DocumentThumbnail(doc = doc, organizationId = organizationId, loader = thumbnailLoader)
                }
                if (selectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.35f),
                            )
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
            }
        },
        trailingContent = {
            if (isBusy) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else if (!selectionMode) {
                Box {
                    IconButton(onClick = { onMenuExpandedChange(true) }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_more))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { onMenuExpandedChange(false) },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_rename)) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                onMenuExpandedChange(false)
                                onRename()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_manage_tags)) },
                            leadingIcon = { Icon(Icons.Default.Sell, contentDescription = null) },
                            onClick = {
                                onMenuExpandedChange(false)
                                onManageTags()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                onMenuExpandedChange(false)
                                onDelete()
                            },
                        )
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            )
            .combinedClickable(enabled = clickEnabled, onClick = onClick, onLongClick = onLongClick),
    )
}

@Composable
private fun RowScope.SwipeActionBackground(
    direction: SwipeToDismissBoxValue,
    startAction: SwipeAction,
    endAction: SwipeAction,
) {
    val action = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> startAction
        SwipeToDismissBoxValue.EndToStart -> endAction
        SwipeToDismissBoxValue.Settled -> SwipeAction.NONE
    }
    val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
    val tint = when (action) {
        SwipeAction.DELETE -> MaterialTheme.colorScheme.error
        SwipeAction.RENAME, SwipeAction.EDIT_TAGS -> MaterialTheme.colorScheme.primary
        SwipeAction.NONE -> MaterialTheme.colorScheme.surfaceVariant
    }
    val icon = when (action) {
        SwipeAction.DELETE -> Icons.Default.Delete
        SwipeAction.RENAME -> Icons.Default.Edit
        SwipeAction.EDIT_TAGS -> Icons.Default.Sell
        SwipeAction.NONE -> null
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tint.copy(alpha = 0.15f))
            .padding(horizontal = 24.dp),
        contentAlignment = alignment,
    ) {
        icon?.let { Icon(it, contentDescription = null, tint = tint) }
    }
}

@Composable
private fun TagChip(tag: TagDto, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(parseHexColor(tag.color).copy(alpha = 0.25f))
            .border(1.dp, parseHexColor(tag.color), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(tag.name, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
internal fun DocumentThumbnail(
    doc: DocumentDto,
    organizationId: String,
    loader: DocumentThumbnailLoader,
) {
    var bitmap by remember(doc.id) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(doc.id, organizationId) {
        if (organizationId.isBlank()) return@LaunchedEffect
        bitmap = loader.load(organizationId, doc)
    }

    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    } else {
        Icon(Icons.Default.Description, contentDescription = null)
    }
}

internal fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.lastIndex) {
        size /= 1024
        unitIndex++
    }
    return if (unitIndex == 0) "${size.toInt()} ${units[unitIndex]}" else "%.1f %s".format(size, units[unitIndex])
}

internal fun formatDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return runCatching {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withZone(java.time.ZoneId.systemDefault())
            .format(Instant.parse(iso))
    }.getOrDefault(iso)
}
