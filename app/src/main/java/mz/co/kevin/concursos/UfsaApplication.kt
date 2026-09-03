package mz.co.kevin.concursos

import android.app.Application
import androidx.room.Room
import mz.co.kevin.concursos.data.ai.GoogleAiService
import mz.co.kevin.concursos.data.local.AppDatabase
import mz.co.kevin.concursos.data.repository.PerfilEmpresaRepository
import mz.co.kevin.concursos.data.repository.UfsaRepository
import mz.co.kevin.concursos.data.settings.SettingsRepository

class UfsaApplication : Application() {
    companion object {
        lateinit var database: AppDatabase
            private set
        lateinit var repository: UfsaRepository
            private set
        lateinit var settings: SettingsRepository
            private set
        lateinit var perfilRepository: PerfilEmpresaRepository
            private set
        lateinit var googleAiService: GoogleAiService
            private set
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "ufsa_app.db"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

        repository = UfsaRepository(
            database.concursoDao(),
            database.fornecedorDao(),
            database.guardadosDao(),
            database.refsVistasDao()
        )
        settings = SettingsRepository(this)
        perfilRepository = PerfilEmpresaRepository(this)
        googleAiService = GoogleAiService()
    }
}
