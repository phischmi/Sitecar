package app.sitecar.uploader.ui.documents

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.sitecar.uploader.R
import app.sitecar.uploader.data.DocumentDto
import app.sitecar.uploader.data.DocumentThumbnailLoader
import app.sitecar.uploader.data.Organization
import app.sitecar.uploader.data.SettingsStore
import app.sitecar.uploader.data.SitecarApiClient
import app.sitecar.uploader.ui.util.ApiErrorMessages
import app.sitecar.uploader.ui.util.friendlyErrorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    client: SitecarApiClient,
    store: SettingsStore,
    onOpenSettings: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val thumbnailLoader = remember(client) {
        DocumentThumbnailLoader(client, File(context.cacheDir, "thumbnails"))
    }

    var orgs by remember { mutableStateOf<List<Organization>>(emptyList()) }
    var selectedOrg by remember { mutableStateOf<Organization?>(null) }
    var dropdownOpen by remember { mutableStateOf(false) }

    var documents by remember { mutableStateOf<List<DocumentDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var openingDocumentId by remember { mutableStateOf<String?>(null) }
    var menuOpenForId by remember { mutableStateOf<String?>(null) }
    var restoringDocumentId by remember { mutableStateOf<String?>(null) }
    var deletingDocumentId by remember { mutableStateOf<String?>(null) }
    var docPendingPermanentDelete by remember { mutableStateOf<DocumentDto?>(null) }
    var emptyTrashDialogOpen by remember { mutableStateOf(false) }
    var emptyingTrash by remember { mutableStateOf(false) }
    val apiErrorMessages = ApiErrorMessages(
        unauthorized = stringResource(R.string.error_unauthorized),
        notFound = stringResource(R.string.error_not_found),
        server = stringResource(R.string.error_server),
        noConnection = stringResource(R.string.error_no_connection),
        timeout = stringResource(R.string.error_timeout),
        unknown = stringResource(R.string.error_unknown),
    )

    suspend fun fetchDeleted(org: Organization) {
        loading = true
        errorMessage = null
        client.listDeletedDocuments(organizationId = org.id)
            .onSuccess { documents = it.documents }
            .onFailure { errorMessage = friendlyErrorMessage(it, apiErrorMessages) }
        loading = false
    }

    LaunchedEffect(Unit) {
        client.listOrganizations()
            .onSuccess { list ->
                orgs = list
                selectedOrg = list.firstOrNull { it.id == store.defaultOrgId } ?: list.firstOrNull()
            }
            .onFailure { errorMessage = friendlyErrorMessage(it, apiErrorMessages) }
    }

    LaunchedEffect(selectedOrg) {
        val org = selectedOrg ?: return@LaunchedEffect
        fetchDeleted(org)
    }

    fun restoreDocument(doc: DocumentDto) {
        val org = selectedOrg ?: return
        restoringDocumentId = doc.id
        scope.launch {
            client.restoreDocument(organizationId = org.id, documentId = doc.id)
                .onSuccess { documents = documents.filterNot { it.id == doc.id } }
                .onFailure { errorMessage = friendlyErrorMessage(it, apiErrorMessages) }
            restoringDocumentId = null
        }
    }

    fun deleteForever(doc: DocumentDto) {
        val org = selectedOrg ?: return
        deletingDocumentId = doc.id
        scope.launch {
            client.deleteTrashDocument(organizationId = org.id, documentId = doc.id)
                .onSuccess { documents = documents.filterNot { it.id == doc.id } }
                .onFailure { errorMessage = friendlyErrorMessage(it, apiErrorMessages) }
            deletingDocumentId = null
        }
    }

    fun emptyTrash() {
        val org = selectedOrg ?: return
        emptyingTrash = true
        scope.launch {
            client.emptyTrash(organizationId = org.id)
                .onSuccess { documents = emptyList() }
                .onFailure { errorMessage = friendlyErrorMessage(it, apiErrorMessages) }
            emptyingTrash = false
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
            TopAppBar(
                title = { Text(stringResource(R.string.trash_title)) },
                actions = {
                    if (documents.isNotEmpty()) {
                        if (emptyingTrash) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp).padding(horizontal = 12.dp),
                            )
                        } else {
                            IconButton(onClick = { emptyTrashDialogOpen = true }) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    contentDescription = stringResource(R.string.trash_empty_action),
                                )
                            }
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings))
                    }
                },
            )
        },
        bottomBar = bottomBar,
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
            if (orgs.size > 1) {
                ExposedDropdownMenuBox(
                    expanded = dropdownOpen,
                    onExpandedChange = { dropdownOpen = !dropdownOpen },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    OutlinedTextField(
                        value = selectedOrg?.name.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.documents_org)) },
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
            }

            PullToRefreshBox(
                isRefreshing = loading,
                onRefresh = {
                    val org = selectedOrg
                    if (org != null) {
                        scope.launch { fetchDeleted(org) }
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
                            text = stringResource(R.string.trash_empty),
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
                            ListItem(
                                headlineContent = { Text(doc.name ?: doc.id) },
                                supportingContent = {
                                    Text("${formatDate(doc.createdAt)} · ${formatSize(doc.originalSize)}")
                                },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (openingDocumentId == doc.id) {
                                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                                        } else {
                                            DocumentThumbnail(
                                                doc = doc,
                                                organizationId = orgId,
                                                loader = thumbnailLoader,
                                            )
                                        }
                                    }
                                },
                                trailingContent = {
                                    if (restoringDocumentId == doc.id || deletingDocumentId == doc.id) {
                                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                                    } else {
                                        Box {
                                            IconButton(onClick = { menuOpenForId = doc.id }) {
                                                Icon(
                                                    Icons.Default.MoreVert,
                                                    contentDescription = stringResource(R.string.action_more),
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = menuOpenForId == doc.id,
                                                onDismissRequest = { menuOpenForId = null },
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.trash_restore)) },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.RestoreFromTrash, contentDescription = null)
                                                    },
                                                    onClick = {
                                                        menuOpenForId = null
                                                        restoreDocument(doc)
                                                    },
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.trash_delete_forever)) },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                                                    },
                                                    onClick = {
                                                        menuOpenForId = null
                                                        docPendingPermanentDelete = doc
                                                    },
                                                )
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = openingDocumentId == null) { openDocument(doc) },
                            )
                        }
                    }
                }
            }
        }
    }

    val deleteTarget = docPendingPermanentDelete
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { docPendingPermanentDelete = null },
            title = { Text(stringResource(R.string.trash_delete_forever_title)) },
            text = {
                Text(stringResource(R.string.trash_delete_forever_message, deleteTarget.name ?: deleteTarget.id))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteForever(deleteTarget)
                        docPendingPermanentDelete = null
                    },
                ) {
                    Text(stringResource(R.string.trash_delete_forever))
                }
            },
            dismissButton = {
                TextButton(onClick = { docPendingPermanentDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (emptyTrashDialogOpen) {
        AlertDialog(
            onDismissRequest = { emptyTrashDialogOpen = false },
            title = { Text(stringResource(R.string.trash_empty_confirm_title)) },
            text = { Text(stringResource(R.string.trash_empty_confirm_message, documents.size)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        emptyTrashDialogOpen = false
                        emptyTrash()
                    },
                ) {
                    Text(stringResource(R.string.trash_empty_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { emptyTrashDialogOpen = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
