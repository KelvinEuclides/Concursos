package mz.co.kevin.concursos.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Guarda as [AppSettings] em SharedPreferences e publica-as num [StateFlow].
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("ufsa_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(ler())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    /** Snapshot síncrono — útil no worker, que não observa fluxos. */
    fun atual(): AppSettings = ler()

    private fun ler(): AppSettings = AppSettings(
        tema = runCatching { TemaApp.valueOf(prefs.getString(KEY_TEMA, null) ?: TemaApp.SISTEMA.name) }
            .getOrDefault(TemaApp.SISTEMA),
        coresDinamicas = prefs.getBoolean(KEY_CORES_DINAMICAS, true),
        notificacoesHabilitadas = prefs.getBoolean(KEY_NOTIF_HABILITADAS, true),
        notificarApenasTI = prefs.getBoolean(KEY_NOTIF_APENAS_TI, false),
        intervaloHoras = prefs.getInt(KEY_INTERVALO_HORAS, 4)
    )

    private inline fun editar(bloco: SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(bloco).apply()
        _settings.value = ler()
    }

    fun definirTema(tema: TemaApp) = editar { putString(KEY_TEMA, tema.name) }
    fun definirCoresDinamicas(ativo: Boolean) = editar { putBoolean(KEY_CORES_DINAMICAS, ativo) }
    fun definirNotificacoesHabilitadas(ativo: Boolean) = editar { putBoolean(KEY_NOTIF_HABILITADAS, ativo) }
    fun definirNotificarApenasTI(ativo: Boolean) = editar { putBoolean(KEY_NOTIF_APENAS_TI, ativo) }
    fun definirIntervaloHoras(horas: Int) = editar { putInt(KEY_INTERVALO_HORAS, horas) }

    private companion object {
        const val KEY_TEMA = "tema"
        const val KEY_CORES_DINAMICAS = "cores_dinamicas"
        const val KEY_NOTIF_HABILITADAS = "notif_habilitadas"
        const val KEY_NOTIF_APENAS_TI = "notif_apenas_ti"
        const val KEY_INTERVALO_HORAS = "intervalo_horas"
    }
}
