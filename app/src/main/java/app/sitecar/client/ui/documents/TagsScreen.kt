package app.sitecar.client.ui.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.sitecar.client.R
import app.sitecar.client.data.Organization
import app.sitecar.client.data.SettingsStore
import app.sitecar.client.data.SitecarApiClient
import app.sitecar.client.data.TagDto
import app.sitecar.client.ui.util.friendlyErrorMessage
import app.sitecar.client.ui.util.rememberApiErrorMessages
import kotlinx.coroutines.launch

private val TAG_COLORS = listOf(
    "#D8FF75", "#7FFF7A", "#7AFFCE", "#7AD7FF", "#7A7FFF",
    "#CE7AFF", "#FF7AD7", "#FF7A7F", "#FFCE7A", "#FFFFFF",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    client: SitecarApiClient,
    store: SettingsStore,
    onOpenSettings: () -> Unit,
    onFilterByTag: (String) -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    var orgs by remember { mutableStateOf<List<Organization>>(emptyList()) }
    var selectedOrg by remember { mutableStateOf<Organization?>(null) }
    var dropdownOpen by remember { mutableStateOf(false) }

    var tags by remember { mutableStateOf<List<TagDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var createDialogOpen by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    var newTagColor by remember { mutableStateOf(TAG_COLORS.first()) }
    var newTagDescription by remember { mutableStateOf("") }
    var creatingTag by remember { mutableStateOf(false) }

    var tagPendingDelete by remember { mutableStateOf<TagDto?>(null) }
    var deletingTagId by remember { mutableStateOf<String?>(null) }

    var editingTag by remember { mutableStateOf<TagDto?>(null) }
    var editTagName by remember { mutableStateOf("") }
    var editTagColor by remember { mutableStateOf(TAG_COLORS.first()) }
    var editTagDescription by remember { mutableStateOf("") }
    var savingTag by remember { mutableStateOf(false) }

    val apiErrorMessages = rememberApiErrorMessages()

    suspend fun fetchTags(org: Organization) {
        loading = true
        errorMessage = null
        client.listTags(organizationId = org.id)
            .onSuccess { tags = it }
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
        fetchTags(org)
    }

    fun createTag() {
        val org = selectedOrg ?: return
        val name = newTagName.trim()
        if (name.isEmpty()) return
        creatingTag = true
        scope.launch {
            client.createTag(
                organizationId = org.id,
                name = name,
                color = newTagColor,
                description = newTagDescription.trim(),
            )
                .onSuccess { tag ->
                    tags = tags + tag
                    createDialogOpen = false
                    newTagName = ""
                    newTagDescription = ""
                    newTagColor = TAG_COLORS.first()
                }
                .onFailure { errorMessage = friendlyErrorMessage(it, apiErrorMessages) }
            creatingTag = false
        }
    }

    fun startEditingTag(tag: TagDto) {
        editingTag = tag
        editTagName = tag.name
        editTagColor = tag.color
        editTagDescription = tag.description.orEmpty()
    }

    fun updateTag() {
        val org = selectedOrg ?: return
        val tag = editingTag ?: return
        val name = editTagName.trim()
        if (name.isEmpty()) return
        savingTag = true
        scope.launch {
            client.updateTag(
                organizationId = org.id,
                tagId = tag.id,
                name = name,
                color = editTagColor,
                description = editTagDescription.trim(),
            )
                .onSuccess { updated ->
                    tags = tags.map { if (it.id == updated.id) updated else it }
                    editingTag = null
                }
                .onFailure { errorMessage = friendlyErrorMessage(it, apiErrorMessages) }
            savingTag = false
        }
    }

    fun deleteTag(tag: TagDto) {
        val org = selectedOrg ?: return
        deletingTagId = tag.id
        scope.launch {
            client.deleteTag(organizationId = org.id, tagId = tag.id)
                .onSuccess { tags = tags.filterNot { it.id == tag.id } }
                .onFailure { errorMessage = friendlyErrorMessage(it, apiErrorMessages) }
            deletingTagId = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tags_title)) },
                actions = {
                    IconButton(onClick = { createDialogOpen = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.tags_create))
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
                        scope.launch { fetchTags(org) }
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
                    tags.isEmpty() && !loading -> Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Default.Sell,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.tags_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { createDialogOpen = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.tags_create))
                        }
                    }
                    tags.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(tags, key = { it.id }) { tag ->
                            ListItem(
                                headlineContent = { Text(tag.name) },
                                supportingContent = {
                                    val subtitle = listOfNotNull(
                                        stringResource(R.string.tags_document_count, tag.documentsCount),
                                        tag.description?.takeIf { it.isNotBlank() },
                                    ).joinToString(" · ")
                                    Text(subtitle)
                                },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(parseHexColor(tag.color))
                                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                                    )
                                },
                                trailingContent = {
                                    if (deletingTagId == tag.id) {
                                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                                    } else {
                                        Row {
                                            IconButton(onClick = { startEditingTag(tag) }) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = stringResource(R.string.action_edit),
                                                )
                                            }
                                            IconButton(onClick = { tagPendingDelete = tag }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = stringResource(R.string.action_delete),
                                                )
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onFilterByTag(buildTagSearchQuery(tag.name)) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (createDialogOpen) {
        AlertDialog(
            onDismissRequest = { if (!creatingTag) createDialogOpen = false },
            title = { Text(stringResource(R.string.tags_create_title)) },
            text = {
                TagFormFields(
                    name = newTagName,
                    onNameChange = { newTagName = it },
                    color = newTagColor,
                    onColorChange = { newTagColor = it },
                    description = newTagDescription,
                    onDescriptionChange = { newTagDescription = it },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newTagName.isNotBlank() && !creatingTag,
                    onClick = { createTag() },
                ) {
                    if (creatingTag) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text(stringResource(R.string.tags_create))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !creatingTag,
                    onClick = { createDialogOpen = false },
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (editingTag != null) {
        AlertDialog(
            onDismissRequest = { if (!savingTag) editingTag = null },
            title = { Text(stringResource(R.string.tags_edit_title)) },
            text = {
                TagFormFields(
                    name = editTagName,
                    onNameChange = { editTagName = it },
                    color = editTagColor,
                    onColorChange = { editTagColor = it },
                    description = editTagDescription,
                    onDescriptionChange = { editTagDescription = it },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = editTagName.isNotBlank() && !savingTag,
                    onClick = { updateTag() },
                ) {
                    if (savingTag) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text(stringResource(R.string.action_save))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !savingTag,
                    onClick = { editingTag = null },
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    val deleteTarget = tagPendingDelete
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { tagPendingDelete = null },
            title = { Text(stringResource(R.string.tags_delete_title)) },
            text = { Text(stringResource(R.string.tags_delete_message, deleteTarget.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteTag(deleteTarget)
                        tagPendingDelete = null
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { tagPendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun TagFormFields(
    name: String,
    onNameChange: (String) -> Unit,
    color: String,
    onColorChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
) {
    Column {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            singleLine = true,
            label = { Text(stringResource(R.string.tags_name_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.tags_color_label),
            style = MaterialTheme.typography.labelLarge,
        )
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp)) {
            TAG_COLORS.forEach { colorHex ->
                ColorSwatch(
                    colorHex = colorHex,
                    selected = colorHex == color,
                    onClick = { onColorChange(colorHex) },
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(R.string.tags_description_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ColorSwatch(colorHex: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(32.dp)
            .clip(CircleShape)
            .background(parseHexColor(colorHex))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    )
}

internal fun parseHexColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)

/** Baut denselben Filter-Syntax wie die Papra-Weboberfläche, z. B. `tag:Rechnung` oder `tag:"Meine Steuer"`. */
internal fun buildTagSearchQuery(tagName: String): String {
    val value = if (tagName.contains(' ')) {
        "\"" + tagName.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    } else {
        tagName
    }
    return "tag:$value"
}
