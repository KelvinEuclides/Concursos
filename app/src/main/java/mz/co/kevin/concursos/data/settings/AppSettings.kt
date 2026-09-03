package mz.co.kevin.concursos.data.settings

import androidx.annotation.StringRes
import mz.co.kevin.concursos.R

enum class TemaApp(@StringRes val labelRes: Int) {
    SISTEMA(R.string.tema_sistema),
    CLARO(R.string.tema_claro),
    ESCURO(R.string.tema_escuro)
}

/**
 * Preferências do utilizador. Persistidas em [SettingsRepository] (SharedPreferences)
 * e expostas como um fluxo para a UI e o worker reagirem a mudanças.
 */
data class AppSettings(
    val tema: TemaApp = TemaApp.SISTEMA,
    val coresDinamicas: Boolean = true,
    val notificacoesHabilitadas: Boolean = true,
    val notificarApenasTI: Boolean = false,
    val intervaloHoras: Int = 4
) {
    companion object {
        val INTERVALOS_DISPONIVEIS = listOf(4, 8, 12, 24)
    }
}
