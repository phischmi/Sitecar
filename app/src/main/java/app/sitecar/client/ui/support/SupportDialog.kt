package app.sitecar.client.ui.support

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.sitecar.client.R
import app.sitecar.client.data.BillingManager
import app.sitecar.client.ui.util.findActivity

/**
 * Freiwilliger Spenden-Hinweis, erscheint nach ein paar erfolgreichen
 * Uploads (siehe UploadScreen) oder manuell über die Einstellungen.
 */
@Composable
fun SupportDialog(
    billing: BillingManager,
    onDismiss: () -> Unit,
) {
    val activity = LocalContext.current.findActivity()
    val productDetails by billing.productDetails.collectAsState()
    val priceText = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice
        ?: stringResource(R.string.support_price_fallback)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.support_dialog_title)) },
        text = { Text(stringResource(R.string.support_dialog_body)) },
        confirmButton = {
            TextButton(
                onClick = {
                    activity?.let { billing.launchPurchaseFlow(it) }
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.support_dialog_confirm, priceText))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.support_dialog_dismiss))
            }
        },
    )
}
