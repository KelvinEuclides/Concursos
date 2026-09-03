package mz.co.kevin.concursos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import mz.co.kevin.concursos.data.settings.TemaApp
import mz.co.kevin.concursos.ui.screens.MainContainerScreen
import mz.co.kevin.concursos.ui.theme.ConcursosTheme
import mz.co.kevin.concursos.worker.DailySyncWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val permissaoNotificacao =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* opcional */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        solicitarPermissaoNotificacao()

        setContent {
            val settings by UfsaApplication.settings.settings.collectAsStateWithLifecycle()

            val darkTheme = when (settings.tema) {
                TemaApp.SISTEMA -> isSystemInDarkTheme()
                TemaApp.CLARO -> false
                TemaApp.ESCURO -> true
            }

            LaunchedEffect(settings.notificacoesHabilitadas, settings.intervaloHoras) {
                agendarSincronizacao(
                    habilitada = settings.notificacoesHabilitadas,
                    intervaloHoras = settings.intervaloHoras
                )
            }

            ConcursosTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.coresDinamicas
            ) {
                MainContainerScreen()
            }
        }
    }

    private fun solicitarPermissaoNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val concedida = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!concedida) {
                permissaoNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun agendarSincronizacao(habilitada: Boolean, intervaloHoras: Int) {
        val wm = WorkManager.getInstance(this)
        if (!habilitada) {
            wm.cancelUniqueWork(NOME_TRABALHO)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val trabalho = PeriodicWorkRequestBuilder<DailySyncWorker>(
            intervaloHoras.toLong().coerceIn(1, 24), TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        // UPDATE aplica um novo intervalo sem perder o histórico do trabalho único.
        wm.enqueueUniquePeriodicWork(
            NOME_TRABALHO,
            ExistingPeriodicWorkPolicy.UPDATE,
            trabalho
        )
    }

    private companion object {
        const val NOME_TRABALHO = "UfsaSyncWorker"
    }
}
