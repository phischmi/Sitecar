package app.sitecar.client.ui.documents

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import app.sitecar.client.R
import app.sitecar.client.data.ApiException
import app.sitecar.client.data.DocumentDto
import app.sitecar.client.data.DocumentThumbnailLoader
import app.sitecar.client.data.Organization
import app.sitecar.client.data.SettingsStore
import app.sitecar.client.data.SitecarApiClient
import app.sitecar.client.ui.util.appBarInsets
import app.sitecar.client.ui.util.friendlyErrorMessage
import app.sitecar.client.ui.util.rememberApiErrorMessages
import app.sitecar.client.ui.util.scaffoldContentInsets
import kotlinx.coroutines.launch
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
    val apiErrorMessages = rememberApiErrorMessages()
    val trashActionUnsupportedMessage = stringResource(R.string.error_trash_action_unsupported)

    // Papra's server currently rejects every API key (any permissions) for restore,
    // permanent delete, and empty trash — its auth middleware requires an explicit
    // apiKeyPermissions list to accept API-key auth at all, and these three routes
    // don't declare one, so requireAuthentication() always denies API keys here
    // regardless of scope. Not fixable client-side; surface that instead of the
    // misleading "check your API key" message a plain 401 would otherwise show.
    fun trashActionErrorMessage(error: Throwable): String =
        if (error is ApiException && error.status.value == 401) {
            trashActionUnsupportedMessage
        } else {
            friendlyErrorMessage(error, apiErrorMessages)
        }

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
                .onFailure { errorMessage = trashActionErrorMessage(it) }
            restoringDocumentId = null
        }
    }

    fun deleteForever(doc: DocumentDto) {
        val org = selectedOrg ?: return
        deletingDocumentId = doc.id
        scope.launch {
            client.deleteTrashDocument(organizationId = org.id, documentId = doc.id)
                .onSuccess { documents = documents.filterNot { it.id == doc.id } }
                .onFailure { errorMessage = trashActionErrorMessage(it) }
            deletingDocumentId = null
        }
    }

    fun emptyTrash() {
        val org = selectedOrg ?: return
        emptyingTrash = true
        scope.launch {
            client.emptyTrash(organizationId = org.id)
                .onSuccess { documents = emptyList() }
                .onFailure { errorMessage = trashActionErrorMessage(it) }
            emptyingTrash = false
        }
    }

    fun openDocument(doc: DocumentDto) {
        val org = selectedOrg ?: return
        if (openingDocumentId != null) return
        openingDocumentId = doc.id
        scope.launch {
            val failure = openDocumentExternally(context, client, org.id, doc, apiErrorMessages)
            openingDocumentId = null
            if (failure != null) errorMessage = failure
        }
    }

    Scaffold(
        contentWindowInsets = scaffoldContentInsets,
        topBar = {
            TopAppBar(
                windowInsets = appBarInsets,
                title = { Text(stringResource(R.string.trash_title)) },
                actions = {
                    if (documents.isNotEmpty()) {
                        if (emptyingTrash) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.padding(horizontal = 12.dp).size(20.dp),
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
        },
        bottomBar = bottomBar,
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
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
                    errorMessage != null -> ListStateMessage(
                        text = stringResource(R.string.documents_load_failed, errorMessage.orEmpty()),
                        isError = true,
                    )
                    documents.isEmpty() && !loading -> ListStateMessage(stringResource(R.string.trash_empty))
                    documents.isEmpty() -> ListStateMessage()
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
