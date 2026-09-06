package mz.co.kevin.concursos

import mz.co.kevin.concursos.data.util.DocumentoMatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentoMatcherTest {

    private val perfil = listOf(
        "Alvará / Licença Comercial válida",
        "Certidão de Quitação do INSS",
        "Certidão de Quitação Fiscal (DGI / Finanças)",
    )

    @Test
    fun corresponde_por_conteudo_directo() {
        assertTrue(DocumentoMatcher.corresponde(perfil, "Certidão de Quitação do INSS"))
    }

    @Test
    fun corresponde_ignorando_acentos_e_pontuacao() {
        assertTrue(DocumentoMatcher.corresponde(perfil, "certidao de quitacao do inss."))
    }

    @Test
    fun corresponde_por_duas_palavras_significativas() {
        assertTrue(DocumentoMatcher.corresponde(perfil, "Alvará comercial"))
    }

    @Test
    fun nao_corresponde_documento_ausente() {
        assertFalse(DocumentoMatcher.corresponde(perfil, "Garantia provisória"))
        assertFalse(DocumentoMatcher.corresponde(perfil, "Demonstrações financeiras auditadas"))
    }

    @Test
    fun uma_so_palavra_comum_nao_chega() {
        assertFalse(DocumentoMatcher.corresponde(listOf("Certidão de Registo Comercial"), "Alvará comercial"))
    }
}
