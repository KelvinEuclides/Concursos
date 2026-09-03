package mz.co.kevin.concursos.data.settings

enum class TemaApp(val label: String) {
    SISTEMA("Seguir o sistema"),
    CLARO("Claro"),
    ESCURO("Escuro")
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
