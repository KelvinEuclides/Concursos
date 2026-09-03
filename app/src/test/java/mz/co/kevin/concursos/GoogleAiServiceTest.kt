package mz.co.kevin.concursos

import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.data.ai.GoogleAiService
import mz.co.kevin.concursos.data.model.CampoDetalhe
import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.DetalhesConcurso
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Testes puros (JVM) sobre a lógica de parsing/heurística de [GoogleAiService].
 * Não fazem chamadas de rede — exercitam apenas as funções `internal` que
 * transformam texto/JSON em modelos e as respostas locais de fallback.
 *
 * Usa a implementação real de `org.json` (adicionada como `testImplementation`).
 * O resolvedor de strings é injetado com os textos PT reais (sem Android/Context).
 */
class GoogleAiServiceTest {

    private val service = GoogleAiService(resolveString = ::resolverStringPt)

    private fun concurso(
        referencia: String = "REF1",
        objecto: String = "Aquisição de computadores",
        modalidade: String = "Concurso Público",
        ugea: String = "UGEA X",
        provincia: String = "Maputo",
        dataAbertura: String = "2026-02-01",
        dataLancamento: String = "2026-01-01",
        ehInformatica: Boolean = true
    ) = Concurso(
        referencia = referencia,
        modalidade = modalidade,
        objecto = objecto,
        ugea = ugea,
        provincia = provincia,
        dataLancamento = dataLancamento,
        dataAbertura = dataAbertura,
        linkDetalhes = "https://www.ufsa.gov.mz/x",
        categoria = CategoriaConcurso.ABERTO,
        ehInformatica = ehInformatica
    )

    // ---------- limparJson ----------

    @Test
    fun limparJson_remove_cerca_json() {
        assertEquals("{\"a\":1}", service.limparJson("```json\n{\"a\":1}\n```"))
    }

    @Test
    fun limparJson_remove_cerca_simples() {
        assertEquals("[1,2]", service.limparJson("```\n[1,2]\n```"))
    }

    @Test
    fun limparJson_sem_cerca_apenas_apara_espacos() {
        assertEquals("{\"a\":1}", service.limparJson("   {\"a\":1}  "))
    }

    // ---------- parseItemRecomendacao ----------

    @Test
    fun parseItemRecomendacao_le_todos_os_campos() {
        val obj = JSONObject(
            """
            {
              "referencia": "REF123",
              "scoreCompatibilidade": 82,
              "nivelCompatibilidade": "ALTA",
              "resumoAvaliacao": "Bom encaixe",
              "pontosFortes": ["Experiência", "Localização"],
              "documentosExigidosProvaveis": ["Alvará"],
              "documentosEmFalta": ["INSS"],
              "recomendacaoEstrategica": "Concorrer"
            }
            """.trimIndent()
        )
        val r = service.parseItemRecomendacao(obj, "PADRAO")
        assertEquals("REF123", r.referencia)
        assertEquals(82, r.scoreCompatibilidade)
        assertEquals("ALTA", r.nivelCompatibilidade)
        assertEquals("Bom encaixe", r.resumoAvaliacao)
        assertEquals(listOf("Experiência", "Localização"), r.pontosFortes)
        assertEquals(listOf("Alvará"), r.documentosExigidosProvaveis)
        assertEquals(listOf("INSS"), r.documentosEmFalta)
        assertEquals("Concorrer", r.recomendacaoEstrategica)
    }

    @Test
    fun parseItemRecomendacao_usa_referencia_padrao_quando_ausente() {
        val r = service.parseItemRecomendacao(JSONObject("{}"), "PADRAO")
        assertEquals("PADRAO", r.referencia)
    }

    @Test
    fun parseItemRecomendacao_score_default_50_e_limitado_a_0_100() {
        assertEquals(50, service.parseItemRecomendacao(JSONObject("{}"), "R").scoreCompatibilidade)
        assertEquals(
            100,
            service.parseItemRecomendacao(JSONObject("""{"scoreCompatibilidade": 350}"""), "R").scoreCompatibilidade
        )
        assertEquals(
            0,
            service.parseItemRecomendacao(JSONObject("""{"scoreCompatibilidade": -5}"""), "R").scoreCompatibilidade
        )
    }

    @Test
    fun parseItemRecomendacao_deriva_nivel_a_partir_do_score() {
        assertEquals(
            "ALTA",
            service.parseItemRecomendacao(JSONObject("""{"scoreCompatibilidade": 70}"""), "R").nivelCompatibilidade
        )
        assertEquals(
            "MEDIA",
            service.parseItemRecomendacao(JSONObject("""{"scoreCompatibilidade": 40}"""), "R").nivelCompatibilidade
        )
        assertEquals(
            "BAIXA",
            service.parseItemRecomendacao(JSONObject("""{"scoreCompatibilidade": 10}"""), "R").nivelCompatibilidade
        )
    }

    @Test
    fun parseItemRecomendacao_listas_ausentes_ficam_vazias() {
        val r = service.parseItemRecomendacao(JSONObject("""{"referencia":"R"}"""), "R")
        assertTrue(r.pontosFortes.isEmpty())
        assertTrue(r.documentosExigidosProvaveis.isEmpty())
        assertTrue(r.documentosEmFalta.isEmpty())
    }

    // ---------- parseRecomendacoes ----------

    @Test
    fun parseRecomendacoes_array_direto() {
        val json = """
            [
              {"referencia": "A", "scoreCompatibilidade": 90},
              {"referencia": "B", "scoreCompatibilidade": 30}
            ]
        """.trimIndent()
        val r = service.parseRecomendacoes(json)
        assertEquals(2, r.size)
        assertEquals("A", r[0].referencia)
        assertEquals("B", r[1].referencia)
    }

    @Test
    fun parseRecomendacoes_ignora_itens_sem_referencia() {
        val json = """[{"scoreCompatibilidade": 90}, {"referencia": "B"}]"""
        val r = service.parseRecomendacoes(json)
        assertEquals(1, r.size)
        assertEquals("B", r[0].referencia)
    }

    @Test
    fun parseRecomendacoes_objeto_embrulhado_em_concursos() {
        val json = """{"concursos": [{"referencia": "A"}, {"referencia": "B"}]}"""
        assertEquals(2, service.parseRecomendacoes(json).size)
    }

    @Test
    fun parseRecomendacoes_objeto_embrulhado_em_recomendacoes() {
        val json = """{"recomendacoes": [{"referencia": "A"}]}"""
        assertEquals(1, service.parseRecomendacoes(json).size)
    }

    @Test
    fun parseRecomendacoes_objeto_unico() {
        val json = """{"referencia": "SOLO", "scoreCompatibilidade": 55}"""
        val r = service.parseRecomendacoes(json)
        assertEquals(1, r.size)
        assertEquals("SOLO", r[0].referencia)
    }

    @Test
    fun parseRecomendacoes_com_cerca_markdown() {
        val json = "```json\n[{\"referencia\": \"A\"}]\n```"
        assertEquals(1, service.parseRecomendacoes(json).size)
    }

    @Test
    fun parseRecomendacoes_texto_invalido_devolve_lista_vazia() {
        assertTrue(service.parseRecomendacoes("desculpe, não consegui").isEmpty())
        assertTrue(service.parseRecomendacoes("").isEmpty())
    }

    // ---------- gerarRespostaLocal (fallback offline) ----------

    @Test
    fun gerarRespostaLocal_pergunta_sobre_documentos() {
        val txt = service.gerarRespostaLocal(concurso(), null, "Quais os requisitos e documentos?")
        assertTrue(txt.contains("Documentos e Habilitação"))
        assertTrue(txt.contains("INSS"))
    }

    @Test
    fun gerarRespostaLocal_pergunta_sobre_prazo() {
        val txt = service.gerarRespostaLocal(concurso(dataAbertura = "2026-05-10"), null, "Qual a data limite?")
        assertTrue(txt.contains("Prazos e Sessão de Abertura"))
        assertTrue(txt.contains("2026-05-10"))
    }

    @Test
    fun gerarRespostaLocal_pergunta_sobre_objecto() {
        val txt = service.gerarRespostaLocal(concurso(objecto = "Fornecimento de servidores"), null, "O que é este produto?")
        assertTrue(txt.contains("Objecto e Escopo"))
        assertTrue(txt.contains("Fornecimento de servidores"))
    }

    @Test
    fun gerarRespostaLocal_pergunta_generica_usa_campos_dos_detalhes() {
        val detalhes = DetalhesConcurso(
            referencia = "REF1",
            campos = listOf(CampoDetalhe("Regime", "EXCEPCIONAL"), CampoDetalhe("Objecto Geral", "Servidores")),
            linkAnuncio = "",
            linkDocumento = ""
        )
        val txt = service.gerarRespostaLocal(concurso(), detalhes, "Bom dia")
        assertTrue(txt.contains("Informações Gerais do Concurso"))
        assertTrue(txt.contains("Regime: EXCEPCIONAL"))
    }
}

/**
 * Textos PT reais usados por [GoogleAiService.gerarRespostaLocal], para exercitar
 * a heurística offline em testes JVM puros (sem Android/Context/Robolectric).
 */
private fun resolverStringPt(resId: Int, args: Array<out Any?>): String {
    val template = when (resId) {
        R.string.ai_local_docs ->
            "📌 **Documentos e Habilitação Jurídica (Decreto 79/2022):**\n" +
                "• Certidão de Quitação da Segurança Social (INSS)\n" +
                "• Alvará ou Licenciamento Comercial adequado ao objecto: %1\$s"
        R.string.ai_local_prazos ->
            "📅 **Prazos e Sessão de Abertura:**\n• Referência: %1\$s\n" +
                "• Data de Abertura / Limite: %2\$s\n• Entidade Contratante: %3\$s (%4\$s)"
        R.string.ai_local_prazos_lancamento -> "• Data de Lançamento: %1\$s"
        R.string.ai_local_objecto ->
            "📦 **Objecto e Escopo do Fornecimento:**\n• Descrição: %1\$s\n" +
                "• Modalidade: %2\$s\n• Entidade Compradora: %3\$s\n• Província / Região: %4\$s"
        R.string.ai_local_objecto_ti -> "• Categoria: Tecnologias de Informação e Comunicação (TI)"
        R.string.ai_local_geral ->
            "📋 **Informações Gerais do Concurso (%1\$s):**\n• Objecto: %2\$s\n" +
                "• Modalidade: %3\$s\n• Entidade: %4\$s (%5\$s)\n• Data Limite: %6\$s"
        R.string.ai_local_geral_dica -> "💡 Dica"
        R.string.ai_val_do_concurso -> "do concurso"
        R.string.ai_val_ver_anuncio -> "Ver no anúncio"
        else -> "res:$resId"
    }
    return if (args.isEmpty()) template else String.format(Locale.ROOT, template, *args)
}
