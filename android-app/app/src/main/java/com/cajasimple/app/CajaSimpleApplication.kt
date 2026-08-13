package com.cajasimple.app

import android.app.Application
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.room.Room
import com.cajasimple.app.data.local.CajaDatabase
import com.cajasimple.app.data.repository.SalesRepository
import com.cajasimple.app.data.repository.SettingsRepository
import com.cajasimple.app.worker.DriveBackupWorker
import java.util.Locale

class CajaSimpleApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val spanish = Locale.forLanguageTag("es-AR")
        Locale.setDefault(spanish)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = getSystemService(LocaleManager::class.java)
            if (localeManager.applicationLocales.toLanguageTags() != "es-AR") {
                localeManager.applicationLocales = LocaleList.forLanguageTags("es-AR")
            }
        }
        val database = Room.databaseBuilder(this, CajaDatabase::class.java, "caja-simple.db").build()
        container = AppContainer(
            database = database,
            salesRepository = SalesRepository(database.salesDao()),
            settingsRepository = SettingsRepository(this),
        )
        // Recupera el CSV local y cualquier respaldo pendiente cada vez que la app vuelve a abrirse.
        DriveBackupWorker.enqueue(this)
    }
}

data class AppContainer(
    val database: CajaDatabase,
    val salesRepository: SalesRepository,
    val settingsRepository: SettingsRepository,
)
