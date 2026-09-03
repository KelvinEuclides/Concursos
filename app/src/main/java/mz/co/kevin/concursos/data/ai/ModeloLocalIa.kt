package mz.co.kevin.concursos.data.ai

/**
 * Modelos de linguagem locais (on-device) que a app sabe descarregar sem chave
 * de API nem token — repositórios abertos (`gated: false`) do `litert-community`
 * no HuggingFace, já no formato `.task` do MediaPipe LlmInference.
 *
 * O Gemma não entra aqui: todos os repositórios `google/gemma*` são *gated* e
 * exigem token autenticado. Para usar Gemma, o utilizador importa o ficheiro
 * `.task`/`.bin` manualmente.
 */
enum class ModeloLocalIa(
    val id: String,
    val nomeCurto: String,
    val ficheiro: String,
    val url: String,
    val tamanhoAprox: String
) {
    QWEN_1_5B(
        id = "qwen2_5_1_5b",
        nomeCurto = "Qwen2.5 1.5B",
        ficheiro = "qwen2_5-1_5b-it-q8.task",
        url = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/" +
            "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
        tamanhoAprox = "~1,5 GB"
    ),
    TINYLLAMA_1_1B(
        id = "tinyllama_1_1b",
        nomeCurto = "TinyLlama 1.1B",
        ficheiro = "tinyllama-1_1b-chat-q8.task",
        url = "https://huggingface.co/litert-community/TinyLlama-1.1B-Chat-v1.0/resolve/main/" +
            "TinyLlama-1.1B-Chat-v1.0_multi-prefill-seq_q8_ekv1280.task",
        tamanhoAprox = "~1,1 GB"
    );

    companion object {
        val PADRAO = QWEN_1_5B
        fun porId(id: String?): ModeloLocalIa = entries.firstOrNull { it.id == id } ?: PADRAO
    }
}
