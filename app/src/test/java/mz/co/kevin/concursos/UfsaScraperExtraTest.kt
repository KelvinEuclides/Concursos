package mz.co.kevin.concursos

import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.remote.UfsaScraper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Cobertura adicional das funções puras de [UfsaScraper] — deteção de TI,
 * endpoints, resolução de URLs, ecrãs de erro e parsing de detalhe.
 */
class UfsaScraperExtraTest {

    // ---------- ehInformatica ----------

    @Test
    fun ehInformatica_deteta_palavras_do_cluster_ti_sem_distinguir_maiusculas() {
        assertTrue(UfsaScraper.ehInformatica("Aquisição de COMPUTADORES"))
        assertTrue(UfsaScraper.ehInformatica("modalidade", "Fornecimento de Software", "UGEA X"))
        assertTrue(UfsaScraper.ehInformatica("Serviços de rede e fibra"))
    }

    @Test
    fun ehInformatica_falso_para_objectos_nao_ti() {
        assertFalse(UfsaScraper.ehInformatica("Construção de estrada rural"))
        assertFalse(UfsaScraper.ehInformatica("", "", ""))
    }

    // ---------- endpointDe ----------

    @Test
    fun endpointDe_devolve_um_endpoint_distinto_por_categoria() {
        val urls = CategoriaConcurso.entries.map { UfsaScraper.endpointDe(it) }
        assertEquals(urls.size, urls.toSet().size)
        urls.forEach { assertTrue(it.startsWith("https://www.ufsa.gov.mz/")) }
        assertTrue(UfsaScraper.endpointDe(CategoriaConcurso.ADJUDICADO).contains("adjudicacao"))
        assertTrue(UfsaScraper.endpointDe(CategoriaConcurso.CANCELADO).contains("cancelamento"))
    }

    // ---------- urlAbsoluta ----------

    @Test
    fun urlAbsoluta_trata_vazio_absoluto_e_relativo() {
        assertEquals("", UfsaScraper.urlAbsoluta(""))
        assertEquals("https://x.y/z", UfsaScraper.urlAbsoluta("https://x.y/z"))
        assertEquals(
            "https://www.ufsa.gov.mz/query/x.php",
            UfsaScraper.urlAbsoluta("/query/x.php")
        )
        assertEquals(
            "https://www.ufsa.gov.mz/query/x.php",
            UfsaScraper.urlAbsoluta("query/x.php")
        )
    }

    // ---------- validarResposta ----------

    @Test
    fun validarResposta_passa_em_html_normal() {
        UfsaScraper.validarResposta("<table id='lista'><tr><td>ok</td></tr></table>")
    }

    @Test(expected = IOException::class)
    fun validarResposta_lanca_no_erro_de_mysql() {
        UfsaScraper.validarResposta("... Can't connect to local MySQL server through socket ...")
    }

    @Test(expected = IOException::class)
    fun validarResposta_lanca_no_gateway_invalido() {
        UfsaScraper.validarResposta("<h1>Gateway inválido</h1>")
    }

    // ---------- parseConcursos ----------

    private fun linha(ref: String, modalidade: String, objecto: String) = """
        <tr align="left">
          <td><a href="concurso_detalhes.php?referencia=$ref">$modalidade</a></td>
          <td>$objecto</td><td>UGEA Central</td><td>Maputo</td>
          <td>2026-01-01</td><td>2026-02-01</td>
        </tr>
    """.trimIndent()

    @Test
    fun parseConcursos_le_referencia_do_link_categoria_e_flag_ti() {
        val html = "<table id=\"lista\">${linha("R-1", "Concurso Público", "Aquisição de servidores")}</table>"
        val lista = UfsaScraper.parseConcursos(html, CategoriaConcurso.ADJUDICADO)
        assertEquals(1, lista.size)
        val c = lista[0]
        assertEquals("R-1", c.referencia)
        assertEquals("Concurso Público", c.modalidade)
        assertEquals(CategoriaConcurso.ADJUDICADO, c.categoria)
        assertTrue(c.ehInformatica)
        assertEquals(
            "https://www.ufsa.gov.mz/concurso_detalhes.php?referencia=R-1",
            c.linkDetalhes
        )
    }

    @Test
    fun parseConcursos_ignora_linhas_sem_colunas_suficientes() {
        val html = """
            <table id="lista">
              <tr align="left"><td>só uma célula</td></tr>
              ${linha("R-2", "Cotação", "Papel A4")}
            </table>
        """.trimIndent()
        val lista = UfsaScraper.parseConcursos(html, CategoriaConcurso.ABERTO)
        assertEquals(1, lista.size)
        assertEquals("R-2", lista[0].referencia)
        assertFalse(lista[0].ehInformatica)
    }

    @Test
    fun parseConcursos_html_sem_tabela_devolve_vazio() {
        assertTrue(UfsaScraper.parseConcursos("<html><body>nada</body></html>", CategoriaConcurso.ABERTO).isEmpty())
    }

    // ---------- parseDetalhes ----------

    @Test
    fun parseDetalhes_extrai_campos_e_links_de_descarga() {
        val html = """
            <table id="lista">
              <tr><th>Referência:</th><td>R-9</td></tr>
              <tr><th>Objecto:</th><td>Fornecimento de bens</td></tr>
              <tr><td>linha incompleta</td></tr>
            </table>
            <table id="lista2"><tr>
              <td><a href="/query/Baixar_anuncio.php?x=1">Anúncio</a></td>
              <td><a href="query/Baixar_cad_enc.php?x=1">Caderno</a></td>
            </tr></table>
        """.trimIndent()
        val d = UfsaScraper.parseDetalhes(html, "R-9")
        assertEquals("R-9", d.referencia)
        assertEquals(listOf("Referência", "Objecto"), d.campos.map { it.rotulo })
        assertEquals("R-9", d.campos[0].valor)
        assertTrue(d.linkAnuncio.endsWith("/query/Baixar_anuncio.php?x=1"))
        assertTrue(d.linkDocumento.endsWith("/query/Baixar_cad_enc.php?x=1"))
    }

    // ---------- parseDetalhesFornecedor ----------

    @Test
    fun parseDetalhesFornecedor_ignora_grelhas_largas_e_normaliza_certificado() {
        val html = """
            <html><body>
              <table><tr><th>Regime:</th><td>Normal</td></tr></table>
              <table id="lista"><tr align="left">
                <td>a</td><td>b</td><td>c</td><td>d</td><td>e</td><td>f</td>
              </tr></table>
            </body></html>
        """.trimIndent()
        val d = UfsaScraper.parseDetalhesFornecedor(html, "  CERT-1  ")
        assertEquals("CERT-1", d.certificado)
        assertEquals(1, d.campos.size)
        assertEquals("Regime", d.campos[0].rotulo)
        assertNull(d.campos.firstOrNull { it.rotulo == "a" })
    }
}
