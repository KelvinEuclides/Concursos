package mz.co.kevin.concursos

import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.remote.UfsaScraper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Testes puros (JVM) sobre a lógica de scraping — sem rede. Exercitam as funções
 * `internal` de [UfsaScraper] com HTML representativo do portal da UFSA.
 */
class UfsaScraperTest {

    // ---------- parseConcursos ----------

    private val htmlConcursos = """
        <html><body>
        <table id="lista">
          <tr align="left">
            <td><a href="concurso_detalhes.php?referencia=REF123">Concurso Público: REF123</a></td>
            <td>Aquisição de equipamento informático</td>
            <td>UGEA Ministério</td>
            <td>Maputo</td>
            <td>2026-01-01</td>
            <td>2026-02-01</td>
          </tr>
          <tr align="left">
            <td>Ajuste Directo: REF999</td>
            <td>Construção de estrada rural</td>
            <td>Administração Nacional</td>
            <td>Sofala</td>
          </tr>
          <tr align="left"><td>linha</td><td>curta</td></tr>
        </table>
        </body></html>
    """.trimIndent()

    @Test
    fun parseConcursos_extrai_linhas_validas_e_ignora_curtas() {
        val r = UfsaScraper.parseConcursos(htmlConcursos, CategoriaConcurso.ABERTO)
        assertEquals(2, r.size)
    }

    @Test
    fun parseConcursos_le_referencia_e_link_do_anchor() {
        val c = UfsaScraper.parseConcursos(htmlConcursos, CategoriaConcurso.ABERTO)[0]
        assertEquals("REF123", c.referencia)
        assertEquals("Concurso Público", c.modalidade)
        assertEquals("Aquisição de equipamento informático", c.objecto)
        assertEquals("UGEA Ministério", c.ugea)
        assertEquals("Maputo", c.provincia)
        assertEquals("2026-01-01", c.dataLancamento)
        assertEquals("2026-02-01", c.dataAbertura)
        assertEquals(
            "https://www.ufsa.gov.mz/concurso_detalhes.php?referencia=REF123",
            c.linkDetalhes
        )
        assertEquals(CategoriaConcurso.ABERTO, c.categoria)
        assertTrue(c.ehInformatica)
    }

    @Test
    fun parseConcursos_referencia_por_fallback_quando_nao_ha_anchor() {
        val c = UfsaScraper.parseConcursos(htmlConcursos, CategoriaConcurso.ADJUDICADO)[1]
        assertEquals("REF999", c.referencia)
        assertEquals("Ajuste Directo", c.modalidade)
        assertEquals("", c.linkDetalhes)
        assertFalse(c.ehInformatica)
        assertEquals(CategoriaConcurso.ADJUDICADO, c.categoria)
    }

    @Test
    fun parseConcursos_html_vazio_devolve_lista_vazia() {
        assertTrue(UfsaScraper.parseConcursos("<html></html>", CategoriaConcurso.CANCELADO).isEmpty())
    }

    // ---------- parseFornecedores ----------

    private val htmlFornecedores = """
        <table id="lista">
          <tr align="left">
            <td>CERT-1</td><td>Empresa Alfa Lda</td><td>400123456</td>
            <td>Maputo</td><td>2025-03-01</td><td>Informática; Consultoria</td>
          </tr>
          <tr align="left">
            <td></td><td>Sem Certificado</td><td>x</td><td>y</td><td>z</td><td>w</td>
          </tr>
          <tr align="left"><td>CERT-2</td><td>Beta</td><td>12345</td></tr>
        </table>
    """.trimIndent()

    @Test
    fun parseFornecedores_ignora_cert_vazio_e_linhas_curtas() {
        val r = UfsaScraper.parseFornecedores(htmlFornecedores)
        assertEquals(1, r.size)
        val f = r[0]
        assertEquals("CERT-1", f.certificado)
        assertEquals("Empresa Alfa Lda", f.nome)
        assertEquals("400123456", f.nuit)
        assertEquals("Maputo", f.provincia)
        assertEquals("2025-03-01", f.dataInscricao)
        assertEquals("Informática; Consultoria", f.actividades)
        assertEquals("", f.linkDetalhes)
    }

    @Test
    fun parseFornecedores_extrai_link_de_detalhes_e_ignora_rotulo() {
        val html = """
            <table id="lista">
              <tr align="left">
                <td>CERT-9</td><td>Beta Lda</td><td>400999000</td>
                <td>SOFALA</td><td>27/03/2024</td>
                <td><a href="inscritoscef_detalhes.php?referencia=CERT-9">Ramos de Actividade</a></td>
              </tr>
            </table>
        """.trimIndent()
        val f = UfsaScraper.parseFornecedores(html).single()
        assertEquals("CERT-9", f.certificado)
        assertEquals("SOFALA", f.provincia)
        assertEquals("", f.actividades)
        assertEquals(
            "https://www.ufsa.gov.mz/query/inscritoscef_detalhes.php?referencia=CERT-9",
            f.linkDetalhes
        )
    }

    // ---------- parseDetalhesFornecedor ----------

    @Test
    fun parseDetalhesFornecedor_extrai_pares_rotulo_valor() {
        val html = """
            <html><body>
            <table><tr><th>Regime:</th><td>Normal</td></tr>
                   <tr><th>Telefone:</th><td>+258 84 000 0000</td></tr>
                   <tr><th>Email:</th><td>geral@beta.co.mz</td></tr></table>
            <table id="lista"><tr align="left"><td>a</td><td>b</td><td>c</td><td>d</td><td>e</td><td>f</td></tr></table>
            </body></html>
        """.trimIndent()
        val d = UfsaScraper.parseDetalhesFornecedor(html, " CERT-9 ")
        assertEquals("CERT-9", d.certificado)
        assertEquals(3, d.campos.size)
        assertEquals("Regime", d.campos[0].rotulo)
        assertEquals("Normal", d.campos[0].valor)
        assertEquals("+258 84 000 0000", d.valoresDe("telefone", "contacto"))
    }

    // ---------- parseDetalhes ----------

    private val htmlDetalhes = """
        <table id="lista">
          <tr><td width="20"></td><th>Nº do Concurso:</th><th>REF123</th></tr>
          <tr><td width="20"></td><th>Regime:</th><th>EXCEPCIONAL</th></tr>
          <tr><td width="20"></td><th>Objecto Geral:</th><td>Aquisição de servidores</td></tr>
          <tr><td width="20"></td><th>Data de Lancamento:</th><td>2026-09-02</td></tr>
        </table>
        <table id="lista2">
          <tr>
            <td><a href="includes/Baixar_anuncio.php?REFERENCIA=REF123">Descarregar Anuncio</a></td>
            <td><a href="includes/Baixar_cad_enc.php?REFERENCIA=REF123">Descarregar Documento</a></td>
          </tr>
        </table>
    """.trimIndent()

    @Test
    fun parseDetalhes_extrai_pares_rotulo_valor_e_links() {
        val d = UfsaScraper.parseDetalhes(htmlDetalhes, "REF123")
        assertEquals("REF123", d.referencia)
        assertEquals(4, d.campos.size)
        assertEquals("Nº do Concurso", d.campos[0].rotulo)
        assertEquals("REF123", d.campos[0].valor)
        assertEquals("Objecto Geral", d.campos[2].rotulo)
        assertEquals("Aquisição de servidores", d.campos[2].valor)
        assertEquals(
            "https://www.ufsa.gov.mz/includes/Baixar_anuncio.php?REFERENCIA=REF123",
            d.linkAnuncio
        )
        assertEquals(
            "https://www.ufsa.gov.mz/includes/Baixar_cad_enc.php?REFERENCIA=REF123",
            d.linkDocumento
        )
    }

    // ---------- validarResposta ----------

    @Test(expected = IOException::class)
    fun validarResposta_lanca_em_erro_mysql() {
        UfsaScraper.validarResposta("<h1>Can't connect to local MySQL server through socket</h1>")
    }

    @Test(expected = IOException::class)
    fun validarResposta_lanca_em_socket_mysqld() {
        UfsaScraper.validarResposta("erro: /var/run/mysqld/mysqld.sock (2)")
    }

    @Test(expected = IOException::class)
    fun validarResposta_lanca_em_gateway_invalido() {
        UfsaScraper.validarResposta("<title>Gateway inválido | CENFOSS</title>")
    }

    @Test
    fun validarResposta_ok_em_html_normal() {
        UfsaScraper.validarResposta("<table id='lista'><tr><td>ok</td></tr></table>")
    }

    // ---------- helpers ----------

    @Test
    fun ehInformatica_deteta_cluster_ti() {
        assertTrue(UfsaScraper.ehInformatica("Aquisição de computadores"))
        assertTrue(UfsaScraper.ehInformatica("Serviços de TIC para o Estado"))
        assertTrue(UfsaScraper.ehInformatica("Manutenção", "de rede de fibra", ""))
        assertFalse(UfsaScraper.ehInformatica("Construção de ponte metálica"))
    }

    @Test
    fun endpointDe_aponta_para_o_php_correto() {
        assertTrue(UfsaScraper.endpointDe(CategoriaConcurso.ABERTO).contains("Busca_concurso1.php"))
        assertTrue(UfsaScraper.endpointDe(CategoriaConcurso.ADJUDICADO).contains("Busca_adjudicacao.php"))
        assertTrue(UfsaScraper.endpointDe(CategoriaConcurso.CANCELADO).contains("Busca_cancelamento.php"))
    }

    @Test
    fun urlAbsoluta_normaliza_hrefs() {
        assertEquals("", UfsaScraper.urlAbsoluta(""))
        assertEquals("http://x/y", UfsaScraper.urlAbsoluta("http://x/y"))
        assertEquals("https://www.ufsa.gov.mz/a/b.php", UfsaScraper.urlAbsoluta("a/b.php"))
        assertEquals("https://www.ufsa.gov.mz/a/b.php", UfsaScraper.urlAbsoluta("/a/b.php"))
    }
}
