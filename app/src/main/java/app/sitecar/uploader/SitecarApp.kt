package app.sitecar.uploader

import android.app.Application
import app.sitecar.uploader.data.BillingManager
import app.sitecar.uploader.data.PendingUpload
import app.sitecar.uploader.data.SitecarApiClient
import app.sitecar.uploader.data.PdfBuilder
import app.sitecar.uploader.data.SettingsStore
import app.sitecar.uploader.data.duplicates.RecentUploadsStore
import app.sitecar.uploader.icon.LauncherIcon
import app.sitecar.uploader.shortcuts.AppShortcuts
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
        if (Features.SUPPORTER_ENABLED) {
            billingManager.startConnection()
        }
        AppShortcuts.register(applicationContext)
        LauncherIcon.apply(applicationContext, settingsStore.accentColor)
    }
}
