package app.sitecar.client

import android.app.Application
import app.sitecar.client.data.BillingManager
import app.sitecar.client.data.PdfBuilder
import app.sitecar.client.data.PendingUpload
import app.sitecar.client.data.SettingsStore
import app.sitecar.client.data.SitecarApiClient
import app.sitecar.client.data.duplicates.RecentUploadsStore
import app.sitecar.client.icon.LauncherIcon
import app.sitecar.client.share.ShareReceiver
import app.sitecar.client.shortcuts.AppShortcuts
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class SitecarApp : Application() {
    lateinit var settingsStore: SettingsStore
        private set
    lateinit var apiClient: SitecarApiClient
        private set
    lateinit var pdfBuilder: PdfBuilder
        private set
    lateinit var billingManager: BillingManager
        private set
    lateinit var recentUploadsStore: RecentUploadsStore
        private set

    /**
     * In-Memory-Übergabe zwischen Scan-/Share- und Upload-Screen: entweder
     * frisch gescannte Seiten oder ein per Android-Share-Sheet empfangenes
     * fertiges Dokument.
     */
    @Volatile
    var pendingUpload: PendingUpload? = null

    /** In-Memory-Übergabe eines Tag-Filters vom Tags- zum Dokumente-Tab (z. B. "tag:Rechnung"). */
    @Volatile
    var pendingDocumentSearchQuery: String? = null

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        settingsStore = SettingsStore(applicationContext)
        apiClient = SitecarApiClient(settingsStore)
        pdfBuilder = PdfBuilder(applicationContext)
        recentUploadsStore = RecentUploadsStore(applicationContext)
        billingManager = BillingManager(applicationContext, settingsStore)
        // Prüft selbst Features.SUPPORTER_ENABLED und tut sonst nichts.
        billingManager.startConnection()
        AppShortcuts.register(applicationContext)
        LauncherIcon.apply(applicationContext, settingsStore.accentColor)
        ShareReceiver.setEnabled(applicationContext, settingsStore.shareIntentEnabled)
    }
}
