package mz.co.kevin.concursos.data.settings

import androidx.annotation.StringRes
import mz.co.kevin.concursos.R

enum class TemaApp(@StringRes val labelRes: Int) {
    SISTEMA(R.string.tema_sistema),
    CLARO(R.string.tema_claro),
    ESCURO(R.string.tema_escuro)
}

enum class ProvedorIa(
    @StringRes val labelRes: Int,
    @StringRes val descRes: Int
) {
    GEMINI_CLOUD(R.string.provedor_gemini_cloud, R.string.provedor_gemini_cloud_desc),
    GEMMA_LOCAL(R.string.provedor_gemma_local, R.string.provedor_gemma_local_desc)
}

/**
 * Preferências do utilizador. Persistidas em [SettingsRepository] (SharedPreferences)
 * e expostas como um fluxo para a UI e o worker reagirem a mudanças.
 */
data class AppSettings(
    val tema: TemaApp = TemaApp.SISTEMA,
    val coresDinamicas: Boolean = true,
    val notificacoesHabilitadas: Boolean = true,
    val intervaloHoras: Int = 4,
    val provedorIa: ProvedorIa = ProvedorIa.GEMINI_CLOUD
) {
    companion object {
        val INTERVALOS_DISPONIVEIS = listOf(4, 8, 12, 24)
    }
}
