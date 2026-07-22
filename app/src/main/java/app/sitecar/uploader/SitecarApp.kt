package app.sitecar.uploader

import android.app.Application
import app.sitecar.uploader.data.BillingManager
import app.sitecar.uploader.data.SitecarApiClient
import app.sitecar.uploader.data.PdfBuilder
import app.sitecar.uploader.data.SettingsStore
import app.sitecar.uploader.icon.LauncherIcon
import app.sitecar.uploader.shortcuts.AppShortcuts
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import java.io.File

class SitecarApp : Application() {
    lateinit var settingsStore: SettingsStore
        private set
    lateinit var apiClient: SitecarApiClient
        private set
    lateinit var pdfBuilder: PdfBuilder
        private set
    lateinit var billingManager: BillingManager
        private set

    /** In-Memory-Übergabe der aktuell gescannten Seiten zwischen Scan- und Upload-Screen. */
    @Volatile
    var currentScanPages: List<File> = emptyList()

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        settingsStore = SettingsStore(applicationContext)
        apiClient = SitecarApiClient(settingsStore)
        pdfBuilder = PdfBuilder(applicationContext)
        billingManager = BillingManager(applicationContext, settingsStore)
        billingManager.startConnection()
        AppShortcuts.register(applicationContext)
        LauncherIcon.apply(applicationContext, settingsStore.accentColor)
    }
}
