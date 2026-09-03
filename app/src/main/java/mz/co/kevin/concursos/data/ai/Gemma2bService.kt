package mz.co.kevin.concursos.data.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.DetalhesConcurso
import mz.co.kevin.concursos.data.model.PerfilEmpresa
import java.io.File

/**
 * Serviço de execução local de inferência do modelo Gemma 2B via MediaPipe Tasks GenAI.
 * 100% on-device, sem dependência de internet ou chave de API externa.
 */
class Gemma2bService(
    private val context: Context? = null,
    private val modelManager: GemmaModelManager? = null,
    private val resolveString: (resId: Int, args: Array<out Any?>) -> String = { resId, args ->
        if (context == null) ""
        else if (args.isEmpty()) context.getString(resId)
        else context.getString(resId, *args)
    }
) {
    private var llmInference: LlmInference? = null
    private val mutex = Mutex()

    fun isDisponivel(): Boolean = modelManager?.isModeloInstalado() == true

    /**
     * Inicializa a instância do MediaPipe LlmInference carregando os pesos para memória/GPU.
     */
    suspend fun inicializar(): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            inicializarSemLock()
        }
    }

    private fun inicializarSemLock(): Result<Unit> {
        if (llmInference != null) return Result.success(Unit)
        val ctx = context ?: return Result.failure(IllegalStateException("Contexto Android não configurado."))
        val mgr = modelManager ?: return Result.failure(IllegalStateException("ModelManager não configurado."))

        val modelFile = mgr.obterFicheiroModelo()
            ?: return Result.failure(IllegalStateException(resolveString(R.string.gemma_erro_sem_modelo, emptyArray())))

        return try {
            // A partir do MediaPipe 0.10.22 os parâmetros de amostragem
            // (temperature/topK) deixaram de estar em LlmInferenceOptions e
            // passaram para a LlmInferenceSession — ver [gerarResposta].
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(1024)
                .setMaxTopK(TOP_K)
                .build()

            llmInference = LlmInference.createFromOptions(ctx, options)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private companion object {
        const val TOP_K = 40
        const val TEMPERATURE = 0.6f
    }

    /**
     * Executa a inferência para um prompt textual arbitrário.
     */
    suspend fun gerarResposta(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                if (llmInference == null) {
                    val initResult = inicializarSemLock()
                    if (initResult.isFailure) {
                        return@withContext Result.failure(initResult.exceptionOrNull()!!)
                    }
                }

                val motor = llmInference
                    ?: return@withContext Result.failure(IllegalStateException("Motor de inferência não inicializado."))

                val promptFormatado = formatarPromptTurno(prompt)
                // Sessão nova por chamada = pedidos independentes, sem acumular contexto.
                val sessao = LlmInferenceSession.createFromOptions(
                    motor,
                    LlmInferenceSession.LlmInferenceSessionOptions.builder()
                        .setTopK(TOP_K)
                        .setTemperature(TEMPERATURE)
                        .build()
                )
                val resposta = sessao.use {
                    it.addQueryChunk(promptFormatado)
                    it.generateResponse()
                } ?: return@withContext Result.failure(IllegalStateException("A inferência retornou um resultado vazio."))

                Result.success(resposta.trim())
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }
    }

    /**
     * Testa o funcionamento do modelo com uma pergunta de verificação rápida.
     */
    suspend fun testarInferencia(): Result<String> {
        val prompt = "Apresenta-te brevemente em Português e diz como podes ajudar na análise de concursos públicos em Moçambique."
        return gerarResposta(prompt)
    }

    /**
     * Responde a uma questão do utilizador sobre um concurso público usando o Gemma 2B localmente.
     */
    suspend fun responderPerguntaConcurso(
        perfil: PerfilEmpresa?,
        concurso: Concurso,
        detalhes: DetalhesConcurso?,
        pergunta: String
    ): Result<String> {
        val prompt = construirPromptPergunta(perfil, concurso, detalhes, pergunta)
        return gerarResposta(prompt)
    }

    /**
     * Liberta os recursos da GPU e da memória ocupados pela sessão do LLM.
     */
    fun liberarMemoria() {
        try {
            llmInference?.close()
        } catch (_: Throwable) {
        } finally {
            llmInference = null
        }
    }

    /**
     * Formata o prompt seguindo a especificação de turnos do Gemma:
     * <start_of_turn>user
     * ...
     * <end_of_turn>
     * <start_of_turn>model
     */
    internal fun formatarPromptTurno(prompt: String): String {
        return "<start_of_turn>user\n$prompt<end_of_turn>\n<start_of_turn>model\n"
    }

    internal fun construirPromptPergunta(
        perfil: PerfilEmpresa?,
        concurso: Concurso,
        detalhes: DetalhesConcurso?,
        pergunta: String
    ): String {
        val camposTexto = detalhes?.campos?.joinToString("\n") { "- ${it.rotulo}: ${it.valor}" }
            ?.ifBlank { "Nenhum campo adicional extraído" }
            ?: "Detalhes ainda não carregados"

        val dadosEmpresa = if (perfil != null && perfil.configurado) {
            """
            Empresa: ${perfil.nome.ifBlank { "Não informado" }}
            Áreas: ${perfil.areasAtuacao.joinToString(", ").ifBlank { "Geral" }}
            Províncias: ${perfil.provinciasAtuacao.joinToString(", ").ifBlank { "Todas" }}
            Documentos prontos: ${perfil.documentosDisponiveis.joinToString(", ").ifBlank { "Não especificados" }}
            """.trimIndent()
        } else {
            "Sem perfil de empresa configurado"
        }

        return """
            És um assistente especializado em contratação pública e concursos em Moçambique (UFSA / Decreto 79/2022).
            Responde à questão do utilizador com base nas seguintes informações oficiais do concurso:

            Referência: ${concurso.referencia}
            Objecto: ${concurso.objecto}
            Modalidade: ${concurso.modalidade}
            Entidade Contratante (UGEA): ${concurso.ugea}
            Província: ${concurso.provincia}
            Data de Abertura: ${concurso.dataAbertura}
            Data de Lançamento: ${concurso.dataLancamento}
            Área de TI: ${if (concurso.ehInformatica) "Sim" else "Não"}
            Campos do edital:
            $camposTexto

            Perfil da empresa concorrente:
            $dadosEmpresa

            Pergunta do utilizador:
            $pergunta

            Instruções: Responde em Português de Moçambique, de forma direta, clara e concisa.
        """.trimIndent()
    }
}
