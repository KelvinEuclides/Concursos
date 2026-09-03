package mz.co.kevin.concursos.data.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.DetalhesConcurso
import mz.co.kevin.concursos.data.model.PerfilEmpresa
import mz.co.kevin.concursos.data.model.RecomendacaoConcursoIa
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GoogleAiService(
    /** Resolve de recursos de string. Injetável para testes JVM sem Android/Context. */
    private val resolveString: (resId: Int, args: Array<out Any?>) -> String,
    private val client: OkHttpClient = defaultClient()
) {
    constructor(
        context: Context,
        client: OkHttpClient = defaultClient()
    ) : this(
        resolveString = { resId, args ->
            if (args.isEmpty()) context.getString(resId) else context.getString(resId, *args)
        },
        client = client
    )

    companion object {
        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        private const val MODEL_PRIMARY = "gemini-2.5-flash"
        private const val MODEL_FALLBACK = "gemini-1.5-flash"
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
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException(s(R.string.ai_erro_chave_nao_configurada)))
        }
        if (concursos.isEmpty()) {
            return@withContext Result.success(emptyList())
        }

        // Limitamos aos 30 concursos mais recentes para garantir velocidade e não estourar tokens
        val concursosParaAvaliar = concursos.take(30)
        val lotes = concursosParaAvaliar.chunked(LOTE_SELECAO)
        val total = concursosParaAvaliar.size

        val acumulado = mutableListOf<RecomendacaoConcursoIa>()
        var concluidos = 0
        var primeiroErro: Throwable? = null

        for ((indice, lote) in lotes.withIndex()) {
            val prompt = construirPromptSelecao(perfil, lote)
            try {
                val jsonTexto = try {
                    chamarGeminiJson(apiKey, prompt, MODEL_PRIMARY)
                } catch (e: Exception) {
                    chamarGeminiJson(apiKey, prompt, MODEL_FALLBACK)
                }
                acumulado += parseRecomendacoes(jsonTexto)
            } catch (e: Exception) {
                if (indice == 0) primeiroErro = e
            }
            concluidos += lote.size
            onProgresso(concluidos, total)
        }

        if (acumulado.isEmpty() && primeiroErro != null) {
            return@withContext Result.failure(primeiroErro)
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
        if (apiKey.isBlank()) {
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
            val jsonTexto = try {
                chamarGeminiJson(apiKey, prompt, MODEL_PRIMARY)
            } catch (e: Exception) {
                chamarGeminiJson(apiKey, prompt, MODEL_FALLBACK)
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
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException(s(R.string.ai_erro_chave_nao_configurada_individual)))
        }

        val prompt = construirPromptDetalhes(perfil, concurso, detalhes)

        try {
            val jsonTexto = try {
                chamarGeminiJson(apiKey, prompt, MODEL_PRIMARY)
            } catch (e: Exception) {
                chamarGeminiJson(apiKey, prompt, MODEL_FALLBACK)
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

    private fun chamarGemini(apiKey: String, prompt: String, modelo: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelo:generateContent?key=$apiKey"

        val bodyJson = JSONObject().apply {
            val contents = JSONArray().apply {
                val part = JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                }
                put(part)
            }
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
            })
        }

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

        val bodyJson = JSONObject().apply {
            val contents = JSONArray().apply {
                val part = JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                }
                put(part)
            }
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
                put("responseMimeType", "application/json")
            })
        }

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
            val concursosArr = obj.optJSONArray("concursos") ?: obj.optJSONArray("recomendacoes")
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
