package mz.co.kevin.concursos

import mz.co.kevin.concursos.data.model.PerfilEmpresa
import mz.co.kevin.concursos.data.model.PorteEmpresa
import mz.co.kevin.concursos.data.model.RecomendacaoConcursoIa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerfilEmpresaTest {

    @Test
    fun perfilEmpresa_valoresPadrao_estaoCorretos() {
        val p = PerfilEmpresa()
        assertEquals("", p.nome)
        assertEquals("", p.nuit)
        assertEquals(PorteEmpresa.MEDIA, p.porte)
        assertFalse(p.configurado)
        assertTrue(p.documentosDisponiveis.isEmpty())
        assertEquals(listOf("Todas as Províncias"), p.provinciasAtuacao)
    }

    @Test
    fun perfilEmpresa_documentosPadrao_contemDocumentosChaveMocambique() {
        val docs = PerfilEmpresa.DOCUMENTOS_PADRAO
        assertTrue(docs.any { it.contains("CEF", ignoreCase = true) })
        assertTrue(docs.any { it.contains("Alvará", ignoreCase = true) })
        assertTrue(docs.any { it.contains("Fiscal", ignoreCase = true) })
        assertTrue(docs.any { it.contains("INSS", ignoreCase = true) })
    }

    @Test
    fun recomendacaoConcursoIa_calculaNivelCorreto() {
        val rAlta = RecomendacaoConcursoIa(
            referencia = "REF1",
            scoreCompatibilidade = 85,
            nivelCompatibilidade = "ALTA",
            resumoAvaliacao = "Excelente perfil"
        )
        assertEquals(85, rAlta.scoreCompatibilidade)
        assertEquals("ALTA", rAlta.nivelCompatibilidade)
    }
}
