package mz.co.kevin.concursos.data.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.ConcursoGuardado
import mz.co.kevin.concursos.data.model.DetalhesConcurso
import mz.co.kevin.concursos.data.model.FiltroConcursos
import mz.co.kevin.concursos.data.model.PerfilEmpresa
import mz.co.kevin.concursos.data.model.RecomendacaoConcursoIa
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import mz.co.kevin.concursos.data.settings.ProvedorIa
import mz.co.kevin.concursos.data.settings.SettingsRepository
import java.io.IOException
import java.util.concurrent.TimeUnit

class GoogleAiService(
    /** Resolve de recursos de string. Injetável para testes JVM sem Android/Context. */
    private val resolveString: (resId: Int, args: Array<out Any?>) -> String,
    private val client: OkHttpClient = defaultClient(),
    private val gemma2bService: Gemma2bService? = null,
    private val settingsRepository: SettingsRepository? = null
) {
    constructor(
        context: Context,
        client: OkHttpClient = defaultClient(),
        gemma2bService: Gemma2bService? = null,
        settingsRepository: SettingsRepository? = null
    ) : this(
        resolveString = { resId, args ->
            if (args.isEmpty()) context.getString(resId) else context.getString(resId, *args)
        },
        client = client,
        gemma2bService = gemma2bService,
        settingsRepository = settingsRepository
    )

    companion object {
        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        private const val MODEL_PRIMARY = "gemini-2.5-flash"

        // gemini-1.5-flash foi descontinuado da API e responde 404; usamos o 2.0-flash
        // (sem "thinking", rápido) como alternativa quando o primário falha.
        private const val MODEL_FALLBACK = "gemini-2.0-flash"

        private const val MAX_OUTPUT_TOKENS_JSON = 8192
        private const val MAX_OUTPUT_TOKENS_TEXTO = 2048
        private const val LOTE_SELECAO = 10
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        val CATEGORIAS_PADRAO_IA = listOf(
            "Construção & Obras",
            "Tecnologia & TI",
            "Saúde & Medicamentos",
            "Consultoria & Estudos",
            "Transporte & Veículos",
            "Bens & Materiais Gerais",
            "Energia, Água & Saneamento",
            "Segurança & Serviços Gerais",
            "Outros Fornecimentos"
        )
    }

    private fun s(resId: Int): String = resolveString(resId, emptyArray())
    private fun s(resId: Int, vararg args: Any?): String = resolveString(resId, args)

    /**
     * Testa se a chave de API fornecida é válida executando uma chamada mínima.
     */
    suspend fun testarChave(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException(s(R.string.ai_erro_chave_branco)))
        }

        try {
            val prompt = s(R.string.ai_prompt_conexao)
            val resposta = chamarGemini(apiKey, prompt, MODEL_PRIMARY)
            if (resposta.contains("CONEXAO_OK", ignoreCase = true) || resposta.isNotBlank()) {
                Result.success(s(R.string.ai_chave_validada))
            } else {
                Result.failure(IOException(s(R.string.ai_erro_resposta_inesperada, resposta)))
            }
        } catch (e: Exception) {
            // Tentar com modelo fallback se o modelo primário der 404
            try {
                val prompt = s(R.string.ai_prompt_conexao)
                chamarGemini(apiKey, prompt, MODEL_FALLBACK)
                Result.success(s(R.string.ai_chave_validada))
            } catch (fallbackEx: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Analisa uma lista de concursos abertos cruzando com o perfil e documentos da empresa.
     * Retorna os concursos classificados por grau de compatibilidade e recomendações.
     *
     * A análise é feita em lotes de [LOTE_SELECAO] para dar progresso real, reduzir o risco
     * de timeout/estouro de tokens e preservar resultados parciais se um lote falhar.
     */
    suspend fun selecionarConcursos(
        apiKey: String,
        perfil: PerfilEmpresa,
        concursos: List<Concurso>,
        onProgresso: (concluidos: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<List<RecomendacaoConcursoIa>> = withContext(Dispatchers.IO) {
        val usandoGemma = settingsRepository?.atual()?.provedorIa == ProvedorIa.GEMMA_LOCAL
        if (usandoGemma) {
            if (gemma2bService == null || !gemma2bService.isDisponivel()) {
                return@withContext Result.failure(IllegalStateException(s(R.string.gemma_erro_sem_modelo)))
            }
        } else if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException(s(R.string.ai_erro_chave_nao_configurada)))
        }
        if (concursos.isEmpty()) {
            return@withContext Result.success(emptyList())
        }

        // Limitamos aos 30 concursos mais recentes para garantir velocidade e não estourar tokens
        val concursosParaAvaliar = concursos.take(30)
        val lotes = concursosParaAvaliar.chunked(LOTE_SELECAO)
        val total = concursosParaAvaliar.size
        val concluidos = java.util.concurrent.atomic.AtomicInteger(0)

        // Os lotes são independentes: processamos em paralelo para reduzir drasticamente
        // o tempo total (antes eram 3 chamadas sequenciais de ~30-60s cada).
        val resultadosLotes: List<Result<List<RecomendacaoConcursoIa>>> = coroutineScope {
            lotes.map { lote ->
                async {
                    val prompt = construirPromptSelecao(perfil, lote)
                    val res = runCatching {
                        val jsonTexto = if (usandoGemma && gemma2bService != null) {
                            gemma2bService.gerarResposta(prompt).getOrThrow()
                        } else {
                            try {
                                chamarGeminiJson(apiKey, prompt, MODEL_PRIMARY)
                            } catch (e: Exception) {
                                chamarGeminiJson(apiKey, prompt, MODEL_FALLBACK)
                            }
                        }
                        parseRecomendacoes(jsonTexto)
                    }
                    onProgresso(concluidos.addAndGet(lote.size), total)
                    res
                }
            }.awaitAll()
        }

        val acumulado = mutableListOf<RecomendacaoConcursoIa>()
        var ultimoErro: Throwable? = null
        for (r in resultadosLotes) {
            r.onSuccess { acumulado += it }
                .onFailure { ultimoErro = it }
        }

        // Nunca devolver sucesso vazio silencioso: se nada foi analisado, propaga o erro
        // (ou um erro genérico quando as chamadas responderam mas nada era analisável).
        if (acumulado.isEmpty()) {
            return@withContext Result.failure(
                ultimoErro ?: IOException(s(R.string.ai_erro_sem_resultados))
            )
        }

        Result.success(acumulado.sortedByDescending { it.scoreCompatibilidade })
    }

    /**
     * Categoriza concursos abertos em categorias padrão usando Google AI (Gemini).
     * O formato estruturado e a taxonomia padrão evitam alucinações.
     */
    suspend fun categorizarConcursos(
        apiKey: String,
        concursos: List<Concurso>
    ): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        val usandoGemma = settingsRepository?.atual()?.provedorIa == ProvedorIa.GEMMA_LOCAL
        if (usandoGemma) {
            if (gemma2bService == null || !gemma2bService.isDisponivel()) {
                return@withContext Result.failure(IllegalStateException(s(R.string.gemma_erro_sem_modelo)))
            }
        } else if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException(s(R.string.ai_erro_chave_nao_configurada_curto)))
        }
        if (concursos.isEmpty()) {
            return@withContext Result.success(emptyMap())
        }

        val listaAvaliar = concursos.take(50)
        val arrayJson = JSONArray().apply {
            listaAvaliar.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.referencia)
                    put("objecto", c.objecto)
                    put("modalidade", c.modalidade)
                    put("ugea", c.ugea)
                })
            }
        }

        val prompt = s(
            R.string.ai_prompt_categorizar,
            CATEGORIAS_PADRAO_IA.joinToString("\n") { "- $it" },
            arrayJson.toString()
        )

        try {
            val jsonTexto = if (usandoGemma && gemma2bService != null) {
                gemma2bService.gerarResposta(prompt).getOrThrow()
            } else {
                try {
                    chamarGeminiJson(apiKey, prompt, MODEL_PRIMARY)
                } catch (e: Exception) {
                    chamarGeminiJson(apiKey, prompt, MODEL_FALLBACK)
                }
            }

            val mapa = mutableMapOf<String, String>()
            val raiz = JSONObject(limparJson(jsonTexto))
            val arr = raiz.optJSONArray("classificacoes") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val id = item.optString("id").trim()
                val catRecebida = item.optString("categoria").trim()
                if (id.isNotEmpty()) {
                    val categoriaNormalizada = CATEGORIAS_PADRAO_IA.firstOrNull { padrao ->
                        padrao.equals(catRecebida, ignoreCase = true) ||
                            catRecebida.contains(padrao, ignoreCase = true) ||
                            padrao.contains(catRecebida, ignoreCase = true)
                    } ?: "Outros Fornecimentos"
                    mapa[id] = categoriaNormalizada
                }
            }
            Result.success(mapa)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Analisa um concurso individual detalhadamente com base no edital, campos e perfil da empresa.
     */
    suspend fun analisarConcursoDetalhado(
        apiKey: String,
        perfil: PerfilEmpresa,
        concurso: Concurso,
        detalhes: DetalhesConcurso
    ): Result<RecomendacaoConcursoIa> = withContext(Dispatchers.IO) {
        val usandoGemma = settingsRepository?.atual()?.provedorIa == ProvedorIa.GEMMA_LOCAL
        if (usandoGemma) {
            if (gemma2bService == null || !gemma2bService.isDisponivel()) {
                return@withContext Result.failure(IllegalStateException(s(R.string.gemma_erro_sem_modelo)))
            }
        } else if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException(s(R.string.ai_erro_chave_nao_configurada_individual)))
        }

        val prompt = construirPromptDetalhes(perfil, concurso, detalhes)

        try {
            val jsonTexto = if (usandoGemma && gemma2bService != null) {
                gemma2bService.gerarResposta(prompt).getOrThrow()
            } else {
                try {
                    chamarGeminiJson(apiKey, prompt, MODEL_PRIMARY)
                } catch (e: Exception) {
                    chamarGeminiJson(apiKey, prompt, MODEL_FALLBACK)
                }
            }

            val obj = JSONObject(limparJson(jsonTexto))
            val rec = parseItemRecomendacao(obj, concurso.referencia)
            Result.success(rec)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Responde a perguntas específicas do utilizador acerca de um determinado concurso ou produto/serviço.
     */
    suspend fun responderPerguntaConcurso(
        apiKey: String,
        perfil: PerfilEmpresa?,
        concurso: Concurso,
        detalhes: DetalhesConcurso?,
        pergunta: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val usandoGemma = settingsRepository?.atual()?.provedorIa == ProvedorIa.GEMMA_LOCAL
        if (usandoGemma) {
            val gemma = gemma2bService
            if (gemma == null || !gemma.isDisponivel()) {
                return@withContext Result.failure(IllegalStateException(s(R.string.gemma_erro_sem_modelo)))
            }
            return@withContext gemma.responderPerguntaConcurso(perfil, concurso, detalhes, pergunta)
        }

        if (apiKey.isBlank()) {
            val respostaHeuristica = gerarRespostaLocal(concurso, detalhes, pergunta)
            return@withContext Result.success(respostaHeuristica)
        }

        val prompt = construirPromptPergunta(perfil, concurso, detalhes, pergunta)

        try {
            val resposta = try {
                chamarGemini(apiKey, prompt, MODEL_PRIMARY)
            } catch (e: Exception) {
                chamarGemini(apiKey, prompt, MODEL_FALLBACK)
            }
            Result.success(resposta.trim())
        } catch (e: Exception) {
            val fallback = gerarRespostaLocal(concurso, detalhes, pergunta)
            Result.success(s(R.string.ai_nota_offline, e.message ?: "", fallback))
        }
    }

    private fun construirPromptPergunta(
        perfil: PerfilEmpresa?,
        concurso: Concurso,
        detalhes: DetalhesConcurso?,
        pergunta: String
    ): String {
        val camposTexto = detalhes?.campos?.joinToString("\n") { "- ${it.rotulo}: ${it.valor}" }
            ?.ifBlank { s(R.string.ai_campos_nenhum_extraido) }
            ?: s(R.string.ai_campos_nenhum_carregado)

        val dadosEmpresa = if (perfil != null && perfil.configurado) {
            s(
                R.string.ai_prompt_dados_empresa,
                perfil.nome.ifBlank { s(R.string.ai_val_nao_informado) },
                s(perfil.porte.labelRes),
                perfil.areasAtuacao.joinToString(", ").ifBlank { s(R.string.ai_val_geral) },
                perfil.provinciasAtuacao.joinToString(", ").ifBlank { s(R.string.ai_val_todas) },
                perfil.documentosDisponiveis.joinToString(", ").ifBlank { s(R.string.ai_val_nao_especificados) }
            )
        } else {
            s(R.string.ai_prompt_sem_perfil)
        }

        return s(
            R.string.ai_prompt_pergunta,
            concurso.referencia,
            concurso.objecto,
            concurso.modalidade,
            concurso.ugea,
            concurso.provincia,
            concurso.dataLancamento,
            concurso.dataAbertura,
            if (concurso.ehInformatica) s(R.string.ai_val_sim) else s(R.string.ai_val_nao),
            detalhes?.linkAnuncio ?: concurso.linkDetalhes,
            detalhes?.linkDocumento ?: s(R.string.ai_val_nao_informado),
            camposTexto,
            dadosEmpresa,
            pergunta
        )
    }

    /**
     * Interpreta um filtro escrito em linguagem natural (feature #48).
     * Gemma local / chave em branco / erro -> parser de regras offline
     * ([InterpretadorFiltro.local]).
     */
    suspend fun interpretarFiltro(
        apiKey: String,
        texto: String,
        provincias: List<String>,
        categoriasIa: List<String>,
    ): Result<FiltroConcursos> = withContext(Dispatchers.IO) {
        val local = { InterpretadorFiltro.local(texto, provincias, categoriasIa) }
        if (texto.isBlank()) return@withContext Result.success(FiltroConcursos())

        val usandoGemma = settingsRepository?.atual()?.provedorIa == ProvedorIa.GEMMA_LOCAL
        val prompt = s(
            R.string.ai_prompt_filtro_natural,
            provincias.joinToString(", "),
            categoriasIa.joinToString(", "),
            java.time.LocalDate.now().toString(),
            texto,
        )

        val jsonTexto: String = try {
            when {
                usandoGemma -> {
                    val gemma = gemma2bService ?: return@withContext Result.success(local())
                    gemma.gerarResposta(prompt).getOrElse { return@withContext Result.success(local()) }
                }
                apiKey.isBlank() -> return@withContext Result.success(local())
                else -> try {
                    chamarGeminiJson(apiKey, prompt, MODEL_PRIMARY)
                } catch (e: Exception) {
                    chamarGeminiJson(apiKey, prompt, MODEL_FALLBACK)
                }
            }
        } catch (e: Exception) {
            return@withContext Result.success(local())
        }

        Result.success(parseFiltro(jsonTexto, provincias, categoriasIa) ?: local())
    }

    private fun parseFiltro(
        jsonTexto: String,
        provincias: List<String>,
        categoriasIa: List<String>,
    ): FiltroConcursos? = runCatching {
        val o = JSONObject(limparJson(jsonTexto))
        fun opt(k: String) = o.optString(k).trim().ifBlank { null }
        val prov = opt("provincia")?.let { p -> provincias.firstOrNull { it.equals(p, true) } ?: p }
        val cat = opt("categoriaIa")?.let { c -> categoriasIa.firstOrNull { it.equals(c, true) } }
        FiltroConcursos(
            provincia = prov,
            categoriaIa = cat,
            apenasTi = o.optBoolean("apenasTi", false),
            prazoAntesDe = opt("prazoAntesDe")?.takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) },
            termo = opt("termo"),
        )
    }.getOrNull()

    /**
     * Responde a uma pergunta transversal sobre a lista de concursos guardados
     * pelo utilizador. Segue o mesmo esquema de [responderPerguntaConcurso]:
     * Gemma local -> chave em branco (heurística) -> Gemini com fallback.
     */
    suspend fun responderPerguntaGuardados(
        apiKey: String,
        perfil: PerfilEmpresa?,
        guardados: List<ConcursoGuardado>,
        pergunta: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        if (guardados.isEmpty()) {
            return@withContext Result.success(s(R.string.guardados_qa_local_nenhum))
        }
        val prompt = construirPromptGuardados(perfil, guardados, pergunta)
        val usandoGemma = settingsRepository?.atual()?.provedorIa == ProvedorIa.GEMMA_LOCAL

        if (usandoGemma) {
            val gemma = gemma2bService
            if (gemma == null || !gemma.isDisponivel()) {
                return@withContext Result.failure(IllegalStateException(s(R.string.gemma_erro_sem_modelo)))
            }
            return@withContext gemma.gerarResposta(prompt).map { it.trim() }
        }

        if (apiKey.isBlank()) {
            return@withContext Result.success(gerarRespostaLocalGuardados(guardados, pergunta))
        }

        try {
            val resposta = try {
                chamarGemini(apiKey, prompt, MODEL_PRIMARY)
            } catch (e: Exception) {
                chamarGemini(apiKey, prompt, MODEL_FALLBACK)
            }
            Result.success(resposta.trim())
        } catch (e: Exception) {
            val fallback = gerarRespostaLocalGuardados(guardados, pergunta)
            Result.success(s(R.string.ai_nota_offline, e.message ?: "", fallback))
        }
    }

    private fun construirPromptGuardados(
        perfil: PerfilEmpresa?,
        guardados: List<ConcursoGuardado>,
        pergunta: String,
    ): String {
        val dadosEmpresa = if (perfil != null && perfil.configurado) {
            s(
                R.string.ai_prompt_dados_empresa,
                perfil.nome.ifBlank { s(R.string.ai_val_nao_informado) },
                s(perfil.porte.labelRes),
                perfil.areasAtuacao.joinToString(", ").ifBlank { s(R.string.ai_val_geral) },
                perfil.provinciasAtuacao.joinToString(", ").ifBlank { s(R.string.ai_val_todas) },
                perfil.documentosDisponiveis.joinToString(", ").ifBlank { s(R.string.ai_val_nao_especificados) },
            )
        } else {
            s(R.string.ai_prompt_sem_perfil)
        }

        val arr = JSONArray()
        guardados.take(40).forEach { g ->
            arr.put(JSONObject().apply {
                put("referencia", g.referencia)
                put("objecto", g.objecto.ifBlank { g.modalidade })
                put("ugea", g.ugea)
                put("provincia", g.provincia)
                put("prazo", g.dataFimSubmissao)
                put("ehInformatica", g.ehInformatica)
                put("requisitos", g.requisitos.take(220))
            })
        }
        return s(R.string.ai_prompt_pergunta_guardados, dadosEmpresa, arr.toString(2), pergunta)
    }

    /** Heurística offline: prazos, requisitos por palavra-chave, ou listagem. */
    internal fun gerarRespostaLocalGuardados(
        guardados: List<ConcursoGuardado>,
        pergunta: String,
    ): String {
        val p = pergunta.lowercase()
        fun linha(g: ConcursoGuardado) =
            "• ${g.referencia} — ${g.objecto.ifBlank { g.modalidade }}" +
                if (g.dataFimSubmissao.isNotBlank()) " (${g.dataFimSubmissao})" else ""

        val ehPrazo = listOf("prazo", "fecha", "fecham", "data", "limite", "quando", "deadline", "close", "closes", "date")
            .any { it in p }
        val ehDoc = listOf("document", "requisit", "certid", "certificate", "certif", "inss", "alvar", "nuit", "habilit")
            .any { it in p }

        return when {
            ehPrazo -> {
                val ordenados = guardados
                    .filter { it.dataFimSubmissao.isNotBlank() }
                    .sortedBy { it.dataFimSubmissao }
                    .ifEmpty { guardados }
                s(R.string.guardados_qa_local_prazos) + "\n" + ordenados.joinToString("\n") { linha(it) }
            }
            ehDoc -> {
                val termo = Regex("[a-zà-ú]{4,}").findAll(p)
                    .map { it.value }
                    .firstOrNull { it !in setOf("quais", "meus", "guardados", "pedem", "exigem", "precisa", "which", "need", "require", "requirements", "documento", "documentos") }
                    ?: "documento"
                val hits = guardados.filter { it.requisitos.contains(termo, ignoreCase = true) }
                if (hits.isEmpty()) s(R.string.guardados_qa_local_nenhum)
                else s(R.string.guardados_qa_local_docs, termo) + "\n" + hits.joinToString("\n") { linha(it) }
            }
            else -> s(R.string.guardados_qa_local_geral, guardados.size) + "\n" +
                guardados.joinToString("\n") { linha(it) }
        }
    }

    internal fun gerarRespostaLocal(
        concurso: Concurso,
        detalhes: DetalhesConcurso?,
        pergunta: String
    ): String {
        val p = pergunta.lowercase()
        return when {
            "requisito" in p || "document" in p || "certid" in p || "habilit" in p ||
                "requirement" in p || "eligib" in p -> {
                s(R.string.ai_local_docs, concurso.objecto.ifBlank { s(R.string.ai_val_do_concurso) })
            }
            "data" in p || "prazo" in p || "abertura" in p || "limite" in p || "quando" in p ||
                "deadline" in p || "date" in p || "when" in p -> {
                buildString {
                    append(
                        s(
                            R.string.ai_local_prazos,
                            concurso.referencia,
                            concurso.dataAbertura.ifBlank { s(R.string.ai_val_ver_anuncio) },
                            concurso.ugea,
                            concurso.provincia
                        )
                    )
                    if (concurso.dataLancamento.isNotBlank()) {
                        append("\n")
                        append(s(R.string.ai_local_prazos_lancamento, concurso.dataLancamento))
                    }
                }
            }
            "produto" in p || "serviço" in p || "servico" in p || "obra" in p || "objecto" in p ||
                "que é" in p || "o que" in p || "product" in p || "service" in p || "scope" in p -> {
                buildString {
                    append(
                        s(
                            R.string.ai_local_objecto,
                            concurso.objecto,
                            concurso.modalidade,
                            concurso.ugea,
                            concurso.provincia
                        )
                    )
                    if (concurso.ehInformatica) {
                        append("\n")
                        append(s(R.string.ai_local_objecto_ti))
                    }
                }
            }
            else -> {
                buildString {
                    append(
                        s(
                            R.string.ai_local_geral,
                            concurso.referencia,
                            concurso.objecto,
                            concurso.modalidade,
                            concurso.ugea,
                            concurso.provincia,
                            concurso.dataAbertura.ifBlank { s(R.string.ai_val_ver_anuncio) }
                        )
                    )
                    detalhes?.campos?.take(4)?.forEach {
                        append("\n• ${it.rotulo}: ${it.valor}")
                    }
                    append("\n\n")
                    append(s(R.string.ai_local_geral_dica))
                }
            }
        }
    }

    private fun construirPromptSelecao(perfil: PerfilEmpresa, concursos: List<Concurso>): String {
        val docsEmpresa = if (perfil.documentosDisponiveis.isEmpty()) {
            s(R.string.ai_val_nenhum_doc_formal)
        } else {
            perfil.documentosDisponiveis.joinToString(", ")
        }

        val areasEmpresa = if (perfil.areasAtuacao.isEmpty()) {
            s(R.string.ai_val_geral_nao_especificado)
        } else {
            perfil.areasAtuacao.joinToString(", ")
        }

        val provsEmpresa = if (perfil.provinciasAtuacao.isEmpty()) {
            s(R.string.ai_val_todas_provincias)
        } else {
            perfil.provinciasAtuacao.joinToString(", ")
        }

        val listaConcursosJson = JSONArray()
        for (c in concursos) {
            val cObj = JSONObject().apply {
                put("referencia", c.referencia)
                put("objecto", c.objecto.ifBlank { c.modalidade })
                put("modalidade", c.modalidade)
                put("ugea", c.ugea)
                put("provincia", c.provincia)
                put("dataAbertura", c.dataAbertura)
                put("ehInformatica", c.ehInformatica)
            }
            listaConcursosJson.put(cObj)
        }

        return s(
            R.string.ai_prompt_selecao,
            perfil.nome.ifBlank { s(R.string.ai_val_empresa_concorrente) },
            perfil.nuit.ifBlank { s(R.string.ai_val_nao_informado) },
            s(perfil.porte.labelRes),
            areasEmpresa,
            perfil.especialidadesTexto.ifBlank { s(R.string.ai_val_nenhuma_descricao) },
            provsEmpresa,
            docsEmpresa,
            perfil.outrosDocumentos.ifBlank { s(R.string.ai_val_nenhum) },
            listaConcursosJson.toString(2)
        )
    }

    private fun construirPromptDetalhes(
        perfil: PerfilEmpresa,
        concurso: Concurso,
        detalhes: DetalhesConcurso
    ): String {
        val camposTexto = detalhes.campos.joinToString("\n") { "- ${it.rotulo}: ${it.valor}" }
        val docsEmpresa = perfil.documentosDisponiveis.joinToString(", ").ifBlank { s(R.string.ai_val_nao_especificados) }

        return s(
            R.string.ai_prompt_detalhes,
            perfil.nome.ifBlank { s(R.string.ai_val_empresa_concorrente) },
            s(perfil.porte.labelRes),
            perfil.areasAtuacao.joinToString(", "),
            perfil.provinciasAtuacao.joinToString(", "),
            docsEmpresa,
            perfil.outrosDocumentos,
            concurso.referencia,
            concurso.objecto,
            concurso.modalidade,
            concurso.ugea,
            concurso.provincia,
            concurso.dataAbertura,
            camposTexto
        )
    }

    /**
     * Constrói o corpo da requisição. [maxTokens] limita a saída e, nos modelos "2.5"
     * (que fazem "thinking" por omissão e adicionam muita latência), desliga o thinking.
     */
    private fun construirCorpo(prompt: String, modelo: String, maxTokens: Int, json: Boolean): JSONObject =
        JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
                put("maxOutputTokens", maxTokens)
                if (json) put("responseMimeType", "application/json")
                if (modelo.contains("2.5")) {
                    put("thinkingConfig", JSONObject().apply { put("thinkingBudget", 0) })
                }
            })
        }

    private fun chamarGemini(apiKey: String, prompt: String, modelo: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelo:generateContent?key=$apiKey"

        val bodyJson = construirCorpo(prompt, modelo, MAX_OUTPUT_TOKENS_TEXTO, json = false)

        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val respostaCorpo = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val erroMsg = extrairMensagemErro(respostaCorpo, response.code)
                throw IOException(erroMsg)
            }

            return extrairTextoResposta(respostaCorpo)
        }
    }

    private fun chamarGeminiJson(apiKey: String, prompt: String, modelo: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelo:generateContent?key=$apiKey"

        val bodyJson = construirCorpo(prompt, modelo, MAX_OUTPUT_TOKENS_JSON, json = true)

        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val respostaCorpo = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val erroMsg = extrairMensagemErro(respostaCorpo, response.code)
                throw IOException(erroMsg)
            }

            return extrairTextoResposta(respostaCorpo)
        }
    }

    private fun extrairTextoResposta(respostaJson: String): String {
        val json = JSONObject(respostaJson)
        val candidates = json.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            throw IOException(s(R.string.ai_erro_sem_candidatos))
        }
        val content = candidates.getJSONObject(0).optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        if (parts == null || parts.length() == 0) {
            throw IOException(s(R.string.ai_erro_sem_conteudo))
        }
        return parts.getJSONObject(0).optString("text", "")
    }

    private fun extrairMensagemErro(corpo: String, code: Int): String {
        return try {
            val json = JSONObject(corpo)
            val erroObj = json.optJSONObject("error")
            val msg = erroObj?.optString("message")
            if (!msg.isNullOrBlank()) {
                s(R.string.ai_erro_http_msg, code, msg)
            } else {
                s(R.string.ai_erro_http, code)
            }
        } catch (_: Exception) {
            s(R.string.ai_erro_http, code)
        }
    }

    internal fun limparJson(texto: String): String {
        var limpo = texto.trim()
        if (limpo.startsWith("```json")) {
            limpo = limpo.removePrefix("```json")
        } else if (limpo.startsWith("```")) {
            limpo = limpo.removePrefix("```")
        }
        if (limpo.endsWith("```")) {
            limpo = limpo.removeSuffix("```")
        }
        return limpo.trim()
    }

    internal fun parseRecomendacoes(jsonTexto: String): List<RecomendacaoConcursoIa> {
        val limpo = limparJson(jsonTexto)
        val lista = mutableListOf<RecomendacaoConcursoIa>()

        if (limpo.startsWith("[")) {
            val arr = JSONArray(limpo)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val ref = obj.optString("referencia", "")
                if (ref.isNotBlank()) {
                    lista.add(parseItemRecomendacao(obj, ref))
                }
            }
        } else if (limpo.startsWith("{")) {
            val obj = JSONObject(limpo)
            // Aceita qualquer chave cujo valor seja um array de objetos (concursos,
            // recomendacoes, resultados, selecao, data, ...), não só as duas conhecidas.
            val concursosArr = obj.optJSONArray("concursos")
                ?: obj.optJSONArray("recomendacoes")
                ?: obj.keys().asSequence()
                    .mapNotNull { obj.optJSONArray(it) }
                    .firstOrNull { it.length() > 0 && it.optJSONObject(0) != null }
            if (concursosArr != null) {
                for (i in 0 until concursosArr.length()) {
                    val item = concursosArr.optJSONObject(i) ?: continue
                    val ref = item.optString("referencia", "")
                    if (ref.isNotBlank()) {
                        lista.add(parseItemRecomendacao(item, ref))
                    }
                }
            } else {
                val ref = obj.optString("referencia", "")
                if (ref.isNotBlank()) {
                    lista.add(parseItemRecomendacao(obj, ref))
                }
            }
        }
        return lista
    }

    internal fun parseItemRecomendacao(obj: JSONObject, referenciaPadrao: String): RecomendacaoConcursoIa {
        val ref = obj.optString("referencia", referenciaPadrao)
        val score = obj.optInt("scoreCompatibilidade", 50).coerceIn(0, 100)
        val nivel = obj.optString("nivelCompatibilidade", if (score >= 70) "ALTA" else if (score >= 40) "MEDIA" else "BAIXA")
        val resumo = obj.optString("resumoAvaliacao", "")
        val recomendacao = obj.optString("recomendacaoEstrategica", "")

        val pontosFortes = mutableListOf<String>()
        obj.optJSONArray("pontosFortes")?.let { arr ->
            for (i in 0 until arr.length()) pontosFortes.add(arr.optString(i))
        }

        val exigidos = mutableListOf<String>()
        obj.optJSONArray("documentosExigidosProvaveis")?.let { arr ->
            for (i in 0 until arr.length()) exigidos.add(arr.optString(i))
        }

        val emFalta = mutableListOf<String>()
        obj.optJSONArray("documentosEmFalta")?.let { arr ->
            for (i in 0 until arr.length()) emFalta.add(arr.optString(i))
        }

        return RecomendacaoConcursoIa(
            referencia = ref,
            scoreCompatibilidade = score,
            nivelCompatibilidade = nivel,
            resumoAvaliacao = resumo,
            pontosFortes = pontosFortes,
            documentosExigidosProvaveis = exigidos,
            documentosEmFalta = emFalta,
            recomendacaoEstrategica = recomendacao
        )
    }
}
