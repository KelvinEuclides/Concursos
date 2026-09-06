package mz.co.kevin.concursos

import mz.co.kevin.concursos.data.ai.GoogleAiService
import mz.co.kevin.concursos.data.model.CampoDetalhe
import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.DetalhesConcurso
import mz.co.kevin.concursos.data.model.PerfilEmpresa
import mz.co.kevin.concursos.data.model.RecomendacaoConcursoIa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Builder offline da checklist de proposta (feature #47).
 * JVM puro — resolvedor de strings mínimo.
 */
class ChecklistPropostaTest {

    private val service = GoogleAiService(resolveString = { resId, _ -> "res:$resId" })

    private val concurso = Concurso(
        referencia = "UGEA/2026/001",
        modalidade = "Concurso Público",
        objecto = "Fornecimento de servidores e equipamento de rede",
        ugea = "UGEA X",
        provincia = "Maputo Cidade",
        dataLancamento = "2026-02-01",
        dataAbertura = "2026-03-15",
        linkDetalhes = "",
        categoria = CategoriaConcurso.ABERTO,
        ehInformatica = true,
    )

    private fun detalhes(campos: List<CampoDetalhe>) =
        DetalhesConcurso(referencia = concurso.referencia, campos = campos, linkAnuncio = "", linkDocumento = "")

    @Test
    fun usa_documentos_da_analise_e_marca_disponibilidade() {
        val perfil = PerfilEmpresa(
            documentosDisponiveis = listOf("Alvará / Licença Comercial válida", "Certidão de Quitação do INSS"),
            configurado = true,
        )
        val analise = RecomendacaoConcursoIa(
            referencia = concurso.referencia,
            scoreCompatibilidade = 80,
            nivelCompatibilidade = "ALTA",
            resumoAvaliacao = "",
            documentosExigidosProvaveis = listOf(
                "Alvará comercial",
                "Certidão de quitação do INSS",
                "Garantia provisória",
            ),
        )

        val cl = service.construirChecklistLocal(perfil, concurso, detalhes(emptyList()), analise)

        assertEquals(3, cl.documentos.size)
        assertEquals(listOf("Alvará comercial", "Certidão de quitação do INSS", "Garantia provisória"),
            cl.documentos.map { it.texto })
        assertTrue("alvará devia contar como disponível", cl.documentos[0].disponivel)
        assertTrue("INSS devia contar como disponível", cl.documentos[1].disponivel)
        assertFalse("garantia provisória não está no perfil", cl.documentos[2].disponivel)
        assertEquals(1, cl.documentosEmFalta)
    }

    @Test
    fun sem_analise_cai_nos_documentos_padrao() {
        val cl = service.construirChecklistLocal(PerfilEmpresa(), concurso, detalhes(emptyList()), analise = null)
        assertEquals(PerfilEmpresa.DOCUMENTOS_PADRAO.size, cl.documentos.size)
        assertTrue(cl.documentos.none { it.disponivel })
    }

    @Test
    fun extrai_datas_chave_dos_campos_do_edital() {
        val campos = listOf(
            CampoDetalhe("Objecto", "Servidores"),
            CampoDetalhe("Data de Abertura das Propostas", "2026-03-15 10:00"),
            CampoDetalhe("Prazo de entrega das propostas", "2026-03-14"),
        )
        val cl = service.construirChecklistLocal(PerfilEmpresa(), concurso, detalhes(campos), analise = null)

        assertEquals(2, cl.datasChave.size)
        assertTrue(cl.datasChave.any { it.contains("2026-03-15 10:00") })
    }

    @Test
    fun datas_chave_usam_a_abertura_quando_nao_ha_campos() {
        val cl = service.construirChecklistLocal(PerfilEmpresa(), concurso, detalhes(emptyList()), analise = null)
        assertEquals(1, cl.datasChave.size)
    }

    @Test
    fun esqueleto_tem_as_quatro_seccoes_com_pontos() {
        val cl = service.construirChecklistLocal(PerfilEmpresa(), concurso, detalhes(emptyList()), analise = null)
        assertEquals(4, cl.esqueleto.size)
        assertTrue(cl.esqueleto.all { it.pontos.isNotEmpty() })
    }

    @Test
    fun referencia_e_propagada() {
        val cl = service.construirChecklistLocal(PerfilEmpresa(), concurso, detalhes(emptyList()), analise = null)
        assertEquals("UGEA/2026/001", cl.referencia)
    }
}
