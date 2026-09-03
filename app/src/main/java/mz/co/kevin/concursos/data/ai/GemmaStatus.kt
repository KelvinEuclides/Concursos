package mz.co.kevin.concursos.data.ai

/**
 * Representa os possíveis estados do modelo Gemma 2B no dispositivo do utilizador.
 */
sealed interface GemmaStatus {
    /** O modelo ainda não foi descarregado nem importado. */
    data object NaoInstalado : GemmaStatus

    /** O modelo está atualmente a ser descarregado com dados de progresso. */
    data class Descarregando(
        val progresso: Float,
        val baixadoBytes: Long,
        val totalBytes: Long
    ) : GemmaStatus

    /** O ficheiro do modelo está presente no armazenamento local. */
    data class Instalado(
        val tamanhoBytes: Long,
        val caminho: String
    ) : GemmaStatus

    /** O modelo está a ser carregado para a memória/GPU para inferência. */
    data class Carregando(val mensagem: String) : GemmaStatus

    /** O modelo está em memória e pronto para responder a perguntas. */
    data class Pronto(val caminho: String) : GemmaStatus

    /** Ocorreu um erro durante a transferência, importação ou inferência. */
    data class Erro(val mensagem: String) : GemmaStatus
}
