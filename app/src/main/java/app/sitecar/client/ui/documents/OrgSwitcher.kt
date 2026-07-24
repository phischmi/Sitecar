package app.sitecar.client.ui.documents

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import app.sitecar.client.R
import app.sitecar.client.data.Organization

/**
 * Kompakter Organisation-Umschalter für die TopAppBar (Icon links neben dem
 * Einstellungen-Icon), sichtbar sobald mehr als eine Organisation existiert.
 * Schreibt die Auswahl über [onSelect] in `SettingsStore.defaultOrgId`, damit
 * sie app-weit als aktive Organisation übernommen wird.
 */
@Composable
fun OrgSwitcherAction(
    orgs: List<Organization>,
    selectedOrg: Organization?,
    onSelect: (Organization) -> Unit,
) {
    if (orgs.size <= 1) return

    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.CorporateFare, contentDescription = stringResource(R.string.documents_org))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            orgs.forEach { org ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = org.name,
                            fontWeight = if (org.id == selectedOrg?.id) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onSelect(org)
                        expanded = false
                    },
                )
            }
        }
    }
}
