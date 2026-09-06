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
    val coresDinamicas: Boolean = false,
    val notificacoesHabilitadas: Boolean = true,
    /**
     * Quando activo, o worker corre a triagem por IA sobre os concursos novos e
     * só notifica os de alta compatibilidade (score ≥ 70). Sem IA disponível,
     * cai no comportamento normal (notifica todos os novos). Ver feature #46.
     */
    val notificarApenasRelevantesIa: Boolean = false,
    val intervaloHoras: Int = 4,
    val provedorIa: ProvedorIa = ProvedorIa.GEMINI_CLOUD
) {
    companion object {
        val INTERVALOS_DISPONIVEIS = listOf(4, 8, 12, 24)
    }
}
