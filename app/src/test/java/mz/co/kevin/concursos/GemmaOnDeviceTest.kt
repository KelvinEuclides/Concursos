package mz.co.kevin.concursos

import mz.co.kevin.concursos.data.ai.Gemma2bService
import mz.co.kevin.concursos.data.ai.GemmaModelManager
import mz.co.kevin.concursos.data.ai.GemmaStatus
import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.PerfilEmpresa
import mz.co.kevin.concursos.data.settings.AppSettings
import mz.co.kevin.concursos.data.settings.ProvedorIa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GemmaOnDeviceTest {

    private fun criarConcurso(
        referencia: String = "UFSA/2026/001",
        objecto: String = "Fornecimento de equipamentos informáticos para as escolas de Maputo",
        modalidade: String = "Concurso Público",
        ugea: String = "Ministério da Educação",
        provincia: String = "Maputo",
        ehInformatica: Boolean = true
    ) = Concurso(
        referencia = referencia,
        modalidade = modalidade,
        objecto = objecto,
        ugea = ugea,
        provincia = provincia,
        dataLancamento = "2026-01-15",
        dataAbertura = "2026-02-28",
        linkDetalhes = "https://www.ufsa.gov.mz/concurso/1",
        categoria = CategoriaConcurso.ABERTO,
        ehInformatica = ehInformatica
    )

    @Test
    fun testFormatarTamanhoGemma() {
        assertEquals("0 MB", GemmaModelManager.formatarTamanho(0))
        assertEquals("500.0 MB", GemmaModelManager.formatarTamanho(500L * 1024 * 1024))
        assertEquals("1.30 GB", GemmaModelManager.formatarTamanho((1.3 * 1024 * 1024 * 1024).toLong()))
        assertEquals("2.00 GB", GemmaModelManager.formatarTamanho(2L * 1024 * 1024 * 1024))
    }

    @Test
    fun testAppSettingsProvedorIaPadrao() {
        val settings = AppSettings()
        assertEquals(ProvedorIa.GEMINI_CLOUD, settings.provedorIa)
    }

    @Test
    fun testFormatarPromptTurnoGemma() {
        // Criar serviço com dummy resolver
        val service = Gemma2bService(
            context = null as android.content.Context?,
            modelManager = null as GemmaModelManager?,
            resolveString = { _, _ -> "" }
        )

        val prompt = "Olá Gemma, podes resumir o edital?"
        val formatado = service.formatarPromptTurno(prompt)

        assertTrue(formatado.startsWith("<start_of_turn>user\n"))
        assertTrue(formatado.contains(prompt))
        assertTrue(formatado.endsWith("<end_of_turn>\n<start_of_turn>model\n"))
    }

    @Test
    fun testConstruirPromptPerguntaGemma() {
        val service = Gemma2bService(
            context = null as android.content.Context?,
            modelManager = null as GemmaModelManager?,
            resolveString = { _, _ -> "" }
        )

        val concurso = criarConcurso()
        val perfil = PerfilEmpresa(
            nome = "TechMoz Lda",
            areasAtuacao = listOf("Informática", "Redes"),
            configurado = true
        )

        val prompt = service.construirPromptPergunta(
            perfil = perfil,
            concurso = concurso,
            detalhes = null,
            pergunta = "Qual é o prazo limite para submeter a proposta?"
        )

        assertTrue(prompt.contains("UFSA/2026/001"))
        assertTrue(prompt.contains("Fornecimento de equipamentos informáticos"))
        assertTrue(prompt.contains("Ministério da Educação"))
        assertTrue(prompt.contains("TechMoz Lda"))
        assertTrue(prompt.contains("Qual é o prazo limite para submeter a proposta?"))
        assertTrue(prompt.contains("Português de Moçambique"))
    }
}
