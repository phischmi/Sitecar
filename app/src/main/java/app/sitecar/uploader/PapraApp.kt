package app.sitecar.uploader

import android.app.Application
import app.sitecar.uploader.data.BillingManager
import app.sitecar.uploader.data.PapraClient
import app.sitecar.uploader.data.PdfBuilder
import app.sitecar.uploader.data.SettingsStore
import app.sitecar.uploader.shortcuts.AppShortcuts
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import java.io.File

class PapraApp : Application() {
    lateinit var settingsStore: SettingsStore
        private set
    lateinit var papraClient: PapraClient
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
        papraClient = PapraClient(settingsStore)
        pdfBuilder = PdfBuilder(applicationContext)
        billingManager = BillingManager(applicationContext, settingsStore)
        billingManager.startConnection()
        AppShortcuts.sync(applicationContext, enabled = settingsStore.isSupporter)
    }
}
