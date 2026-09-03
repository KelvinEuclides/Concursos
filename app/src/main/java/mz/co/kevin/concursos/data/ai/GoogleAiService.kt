package mz.co.kevin.concursos.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val MODEL_PRIMARY = "gemini-2.5-flash"
        private const val MODEL_FALLBACK = "gemini-1.5-flash"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    /**
     * Testa se a chave de API fornecida é válida executando uma chamada mínima.
     */
    suspend fun testarChave(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("A chave de API não pode estar em branco."))
        }

        try {
            val prompt = "Responda apenas: CONEXAO_OK"
            val resposta = chamarGemini(apiKey, prompt, MODEL_PRIMARY)
            if (resposta.contains("CONEXAO_OK", ignoreCase = true) || resposta.isNotBlank()) {
                Result.success("Chave validada com sucesso com o Google AI!")
            } else {
                Result.failure(IOException("Resposta inesperada do Google AI: $resposta"))
            }
        } catch (e: Exception) {
            // Tentar com modelo fallback se o modelo primário der 404
            try {
                val prompt = "Responda apenas: CONEXAO_OK"
                val resposta = chamarGemini(apiKey, prompt, MODEL_FALLBACK)
                Result.success("Chave validada com sucesso com o Google AI!")
            } catch (fallbackEx: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Analisa uma lista de concursos abertos cruzando com o perfil e documentos da empresa.
     * Retorna os concursos classificados por grau de compatibilidade e recomendações.
     */
    suspend fun selecionarConcursos(
        apiKey: String,
        perfil: PerfilEmpresa,
        concursos: List<Concurso>
    ): Result<List<RecomendacaoConcursoIa>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Por favor configure a sua chave Google AI (Gemini) nas Definições ou no topo desta aba."))
        }
        if (concursos.isEmpty()) {
            return@withContext Result.success(emptyList())
        }

        // Limitamos aos 30 concursos mais recentes para garantir velocidade e não estourar tokens
        val concursosParaAvaliar = concursos.take(30)

        val prompt = construirPromptSelecao(perfil, concursosParaAvaliar)

        try {
            val jsonTexto = try {
                chamarGeminiJson(apiKey, prompt, MODEL_PRIMARY)
            } catch (e: Exception) {
                chamarGeminiJson(apiKey, prompt, MODEL_FALLBACK)
            }

            val recomendacoes = parseRecomendacoes(jsonTexto)
            Result.success(recomendacoes)
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
            return@withContext Result.failure(IllegalStateException("Configure a sua chave Google AI para usar a análise individual."))
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
            Result.success("*(Nota: Resposta em modo offline - ${e.message})*\n\n$fallback")
        }
    }

    private fun construirPromptPergunta(
        perfil: PerfilEmpresa?,
        concurso: Concurso,
        detalhes: DetalhesConcurso?,
        pergunta: String
    ): String {
        val camposTexto = detalhes?.campos?.joinToString("\n") { "- ${it.rotulo}: ${it.valor}" }
            ?.ifBlank { "Nenhum campo adicional extraído." }
            ?: "Nenhum campo adicional carregado ainda."

        val dadosEmpresa = if (perfil != null && perfil.configurado) {
            """
            === DADOS DA EMPRESA DO USUÁRIO ===
            Nome: ${perfil.nome.ifBlank { "Não informado" }}
            Porte: ${perfil.porte.label}
            Áreas de Atuação: ${perfil.areasAtuacao.joinToString(", ").ifBlank { "Geral" }}
            Províncias: ${perfil.provinciasAtuacao.joinToString(", ").ifBlank { "Todas" }}
            Documentos prontos: ${perfil.documentosDisponiveis.joinToString(", ").ifBlank { "Não especificados" }}
            """.trimIndent()
        } else {
            "Perfil da empresa não especificado."
        }

        return """
        Você é um Consultor Especialista Sénior em Licitações e Contratação Pública em Moçambique (UFSA e Regulamento de Contratação aprovado pelo Decreto n.º 79/2022).
        O utilizador quer tirar dúvidas e fazer uma pergunta direta sobre o seguinte concurso público ou produto/serviço licitado:

        === DADOS DO CONCURSO / PRODUTO ===
        Referência: ${concurso.referencia}
        Objecto (Produto/Serviço/Empreitada): ${concurso.objecto}
        Modalidade: ${concurso.modalidade}
        Entidade Contratante (UGEA): ${concurso.ugea}
        Província / Local de Execução: ${concurso.provincia}
        Data de Lançamento: ${concurso.dataLancamento}
        Data de Abertura / Limite: ${concurso.dataAbertura}
        É da área de Informática/TI: ${if (concurso.ehInformatica) "Sim" else "Não"}
        Link do Anúncio/Edital: ${detalhes?.linkAnuncio ?: concurso.linkDetalhes}
        Link do Caderno de Encargos: ${detalhes?.linkDocumento ?: "Não informado"}
        Especificações e Campos extraídos:
        $camposTexto

        $dadosEmpresa

        === PERGUNTA DO UTILIZADOR ===
        "$pergunta"

        === INSTRUÇÕES PARA A RESPOSTA ===
        1. Responda com clareza, autoridade técnica e objetividade em Português de Moçambique.
        2. Se a pergunta for sobre o produto ou serviço, detalhe o que se pretende adquirir com base no objecto e especificações.
        3. Se for sobre documentos e habilitação, liste de forma estruturada em tópicos os documentos fundamentais conforme o Decreto 79/2022 (Quitação das Finanças/AT, Quitação do INSS, Certidão de Registo Comercial, Alvará/Licença comercial compatível, Declarações de idoneidade, etc.).
        4. Se for sobre prazos, modalidades ou garantias (ex: garantia provisória), explique as regras práticas aplicáveis.
        5. Ofereça conselhos táticos para a formulação de uma proposta competitiva e em conformidade com as exigências da UGEA.
        """.trimIndent()
    }

    internal fun gerarRespostaLocal(
        concurso: Concurso,
        detalhes: DetalhesConcurso?,
        pergunta: String
    ): String {
        val p = pergunta.lowercase()
        return when {
            "requisito" in p || "document" in p || "certid" in p || "habilit" in p -> {
                buildString {
                    appendLine("📌 **Documentos e Habilitação Jurídica (Decreto 79/2022):**")
                    appendLine("• Registo Comercial e Certidão de Registo Definitivo válida")
                    appendLine("• Certidão de Quitação das Obrigações Fiscais (Autoridade Tributária / AT)")
                    appendLine("• Certidão de Quitação da Segurança Social (INSS)")
                    appendLine("• Alvará ou Licenciamento Comercial adequado ao objecto: ${concurso.objecto.ifBlank { "do concurso" }}")
                    appendLine("• Declaração de inexistência de falência ou impedimento legal")
                    appendLine("• Proposta Técnica e Financeira assinada pelo representante legal")
                    appendLine()
                    appendLine("💡 *Para obter análise detalhada dos pontos fortes da sua empresa, adicione a chave do Google AI nas Definições.*")
                }
            }
            "data" in p || "prazo" in p || "abertura" in p || "limite" in p || "quando" in p -> {
                buildString {
                    appendLine("📅 **Prazos e Sessão de Abertura:**")
                    appendLine("• Referência: ${concurso.referencia}")
                    appendLine("• Data de Abertura / Limite: ${concurso.dataAbertura.ifBlank { "Ver no anúncio" }}")
                    if (concurso.dataLancamento.isNotBlank()) {
                        appendLine("• Data de Lançamento: ${concurso.dataLancamento}")
                    }
                    appendLine("• Entidade Contratante: ${concurso.ugea} (${concurso.provincia})")
                    appendLine("⚠️ *As propostas devem ser entregues antes do início da sessão pública de abertura.*")
                }
            }
            "produto" in p || "serviço" in p || "servico" in p || "obra" in p || "objecto" in p || "que é" in p || "o que" in p -> {
                buildString {
                    appendLine("📦 **Objecto e Escopo do Fornecimento:**")
                    appendLine("• Descrição: ${concurso.objecto}")
                    appendLine("• Modalidade: ${concurso.modalidade}")
                    appendLine("• Entidade Compradora: ${concurso.ugea}")
                    appendLine("• Província / Região: ${concurso.provincia}")
                    if (concurso.ehInformatica) {
                        appendLine("• Categoria: Tecnologias de Informação e Comunicação (TI)")
                    }
                }
            }
            else -> {
                buildString {
                    appendLine("📋 **Informações Gerais do Concurso (${concurso.referencia}):**")
                    appendLine("• Objecto: ${concurso.objecto}")
                    appendLine("• Modalidade: ${concurso.modalidade}")
                    appendLine("• Entidade: ${concurso.ugea} (${concurso.provincia})")
                    appendLine("• Data Limite: ${concurso.dataAbertura.ifBlank { "Ver anúncio" }}")
                    detalhes?.campos?.take(4)?.forEach {
                        appendLine("• ${it.rotulo}: ${it.valor}")
                    }
                    appendLine()
                    appendLine("💡 *Dica: Você pode ativar o Google AI Gemini gratuitamente nas Definições para tirar qualquer dúvida personalizada.*")
                }
            }
        }
    }

    private fun construirPromptSelecao(perfil: PerfilEmpresa, concursos: List<Concurso>): String {
        val docsEmpresa = if (perfil.documentosDisponiveis.isEmpty()) {
            "Nenhum documento formal especificado."
        } else {
            perfil.documentosDisponiveis.joinToString(", ")
        }

        val areasEmpresa = if (perfil.areasAtuacao.isEmpty()) {
            "Geral / Não especificado"
        } else {
            perfil.areasAtuacao.joinToString(", ")
        }

        val provsEmpresa = if (perfil.provinciasAtuacao.isEmpty()) {
            "Todas as Províncias"
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

        return """
Você é um Consultor Especialista Sénior em Licitações e Contratação Pública em Moçambique, com domínio do Regulamento de Contratação de Empreitada de Obras Públicas, Fornecimento de Bens e Prestação de Serviços ao Estado (Decreto n.º 79/2022) e dos procedimentos da UFSA (Unidade Funcional de Supervisão das Aquisições).

A sua missão é avaliar os concursos públicos abertos da UFSA abaixo e selecionar/classificar quais são os mais indicados para a empresa concorrer com base no perfil e documentos dela.

=== DADOS E DOCUMENTOS DA EMPRESA ===
Nome: ${perfil.nome.ifBlank { "Empresa Concorrente" }}
NUIT: ${perfil.nuit.ifBlank { "Não informado" }}
Porte: ${perfil.porte.label}
Áreas de Atuação: $areasEmpresa
Especialidades / Descrição: ${perfil.especialidadesTexto.ifBlank { "Nenhuma descrição adicional" }}
Províncias de Atuação / Entrega: $provsEmpresa
Documentos e Certidões que a empresa POSSUI prontos:
$docsEmpresa
Outros documentos / Recursos: ${perfil.outrosDocumentos.ifBlank { "Nenhum" }}

=== CONCURSOS ABERTOS PARA AVALIAR ===
${listaConcursosJson.toString(2)}

=== INSTRUÇÕES DE SAÍDA ===
Retorne OBRIGATORIAMENTE um array JSON de objetos (sem texto antes ou depois).
Ordene a lista dos concursos mais recomendados (maior compatibilidade) para os menos recomendados.
Cada objeto deve conter exatamente os campos:
{
  "referencia": "String com a referência idêntica à do concurso fornecido",
  "scoreCompatibilidade": número inteiro de 0 a 100 indicando a aderência técnica, geográfica e documental,
  "nivelCompatibilidade": "ALTA" (se score >= 70), "MEDIA" (se score entre 40 e 69) ou "BAIXA" (se score < 40),
  "resumoAvaliacao": "Texto conciso explicando em português de Moçambique o motivo da nota, cruzando o objecto e o ramo da empresa",
  "pontosFortes": ["Ponto forte 1", "Ponto forte 2"],
  "documentosExigidosProvaveis": ["Documento típico exigido para este tipo de concurso"],
  "documentosEmFalta": ["Documento(s) que a empresa ainda NÃO listou que possui e que podem ser solicitados no caderno de encargos"],
  "recomendacaoEstrategica": "Conselho prático e objetivo para preparar uma proposta vencedora ou atenção ao prazo de entrega"
}
""".trimIndent()
    }

    private fun construirPromptDetalhes(
        perfil: PerfilEmpresa,
        concurso: Concurso,
        detalhes: DetalhesConcurso
    ): String {
        val camposTexto = detalhes.campos.joinToString("\n") { "- ${it.rotulo}: ${it.valor}" }
        val docsEmpresa = perfil.documentosDisponiveis.joinToString(", ").ifBlank { "Não especificados" }

        return """
Você é um Especialista em Contratação Pública em Moçambique (UFSA / Decreto 79/2022).
Avalie este concurso específico para a seguinte empresa:

=== EMPRESA ===
Nome: ${perfil.nome.ifBlank { "Empresa Concorrente" }}
Porte: ${perfil.porte.label}
Áreas: ${perfil.areasAtuacao.joinToString(", ")}
Províncias: ${perfil.provinciasAtuacao.joinToString(", ")}
Documentos da empresa: $docsEmpresa
Outros: ${perfil.outrosDocumentos}

=== CONCURSO ===
Referência: ${concurso.referencia}
Objecto: ${concurso.objecto}
Modalidade: ${concurso.modalidade}
UGEA / Entidade Contratante: ${concurso.ugea}
Província: ${concurso.provincia}
Data Limite / Abertura: ${concurso.dataAbertura}
Detalhes adicionais extraídos:
$camposTexto

Retorne EXCLUSIVAMENTE um objeto JSON:
{
  "referencia": "${concurso.referencia}",
  "scoreCompatibilidade": número inteiro de 0 a 100,
  "nivelCompatibilidade": "ALTA" | "MEDIA" | "BAIXA",
  "resumoAvaliacao": "Análise detalhada do concurso para o perfil da empresa",
  "pontosFortes": ["Ponto forte 1", "Ponto forte 2"],
  "documentosExigidosProvaveis": ["Documento 1", "Documento 2"],
  "documentosEmFalta": ["Documento que a empresa precisa verificar ou obter"],
  "recomendacaoEstrategica": "Estratégia de preço, consórcio ou preparação de proposta"
}
""".trimIndent()
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
            throw IOException("O Google AI não retornou candidatos válidos.")
        }
        val content = candidates.getJSONObject(0).optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        if (parts == null || parts.length() == 0) {
            throw IOException("Resposta sem conteúdo do Google AI.")
        }
        return parts.getJSONObject(0).optString("text", "")
    }

    private fun extrairMensagemErro(corpo: String, code: Int): String {
        return try {
            val json = JSONObject(corpo)
            val erroObj = json.optJSONObject("error")
            val msg = erroObj?.optString("message")
            if (!msg.isNullOrBlank()) {
                "Google AI ($code): $msg"
            } else {
                "Erro HTTP $code na comunicação com o Google AI."
            }
        } catch (_: Exception) {
            "Erro HTTP $code na comunicação com o Google AI."
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
