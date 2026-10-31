package mz.co.kevin.concursos

import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.remote.UngmScraper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários puros (JVM) para o [UngmScraper], validando a extração
 * de editais da ONU focados em Moçambique a partir do HTML retornado pelo portal.
 */
class UngmScraperTest {

    private val htmlNotices = """
        <div id="noticeMain">
            <input name="__RequestVerificationToken" type="hidden" value="test_token_12345" />
            <div role="row" tabindex="0" data-noticeid="294079" class="tableRow dataRow notice-table">
                <div role="cell" class="tableCell resultTitle">
                    <span class="ungm-title ungm-title--small">
                        Fornecimento de Equipamento Informático e Servidores para Moçambique
                    </span>
                    <a href="/Public/Notice/294079">Ver</a>
                </div>
                <div role="cell" class="tableCell resultInfo1 deadline" data-description="Deadline">
                    <span>11-Jan-2027 23:59
                    (GMT -4.00)</span>
                </div>
                <div role="cell" class="tableCell">
                    <span>16-Mar-2026</span>
                </div>
                <div role="cell" class="tableCell resultAgency">
                    <span>PNUD Moçambique</span>
                </div>
                <div role="cell" class="tableCell">
                    <span><label for="Request for Proposal">Request for Proposal</label></span>
                </div>
                <div role="cell" class="tableCell resultInfo1" data-description="Reference">
                    <span>UNDP-MOZ-00123</span>
                </div>
                <div role="cell" class="tableCell">
                    <span>Moçambique</span>
                </div>
            </div>
            <div role="row" tabindex="0" data-noticeid="288090" class="tableRow dataRow notice-table">
                <div role="cell" class="tableCell resultTitle">
                    <span class="ungm-title ungm-title--small">
                        Construção de Furos de Água em Nampula
                    </span>
                    <a href="/Public/Notice/288090">Ver</a>
                </div>
                <div role="cell" class="tableCell resultInfo1 deadline" data-description="Deadline">
                    <span>31-Dez-2026 12:00</span>
                </div>
                <div role="cell" class="tableCell">
                    <span>01-Abr-2026</span>
                </div>
                <div role="cell" class="tableCell resultAgency">
                    <span>UNICEF</span>
                </div>
                <div role="cell" class="tableCell">
                    <span><label for="Invitation to Bid">Invitation to Bid</label></span>
                </div>
                <div role="cell" class="tableCell resultInfo1" data-description="Reference">
                    <span></span>
                </div>
                <div role="cell" class="tableCell">
                    <span>Multiple destinations</span>
                </div>
            </div>
        </div>
    """.trimIndent()

    private val htmlDetalhes = """
        <div class="ungm-col-sm-12">
            <div class="row">
                <span class="label">Reference:</span>
                <span class="value">UNDP-MOZ-00123</span>
            </div>
            <div class="row">
                <span class="label">Published on:</span>
                <span class="value">16-Mar-2026</span>
            </div>
            <div class="row">
                <span class="label">Deadline on:</span>
                <span class="value">11-Jan-2027 23:59</span>
            </div>
        </div>
        <div class="ungm-list-item ungm-background">
            <div class="title">Description</div>
            <div>Aquisição de infraestrutura computacional e datacenter para agências em Maputo.</div>
        </div>
        <table id="tblLinks">
            <tbody>
                <tr data-id="553064">
                    <td>https://www.ungm.org/files/rfp_undp_moz.pdf</td>
                    <td>Edital RFP Completo</td>
                </tr>
            </tbody>
        </table>
    """.trimIndent()

    @Test
    fun extrairToken_recupera_csrf_token() {
        val token = UngmScraper.extrairToken(htmlNotices)
        assertEquals("test_token_12345", token)
    }

    @Test
    fun parseConcursos_extrai_oportunidades_ungm_corretamente() {
        val concursos = UngmScraper.parseConcursos(htmlNotices, CategoriaConcurso.ABERTO)
        assertEquals(2, concursos.size)

        val c1 = concursos[0]
        assertEquals("UNGM-UNDP-MOZ-00123", c1.referencia)
        assertEquals("Request for Proposal", c1.modalidade)
        assertEquals("Fornecimento de Equipamento Informático e Servidores para Moçambique", c1.objecto)
        assertEquals("PNUD Moçambique", c1.ugea)
        assertEquals("Moçambique", c1.provincia)
        assertEquals("16-Mar-2026", c1.dataLancamento)
        assertEquals("11-Jan-2027 23:59", c1.dataAbertura)
        assertEquals("https://www.ungm.org/Public/Notice/294079", c1.linkDetalhes)
        assertEquals(CategoriaConcurso.ABERTO, c1.categoria)
        assertEquals("UNGM", c1.fonte)
        assertTrue(c1.ehInformatica)

        val c2 = concursos[1]
        assertEquals("UNGM-288090", c2.referencia) // fallback quando referência vem vazia
        assertEquals("Invitation to Bid", c2.modalidade)
        assertEquals("Construção de Furos de Água em Nampula", c2.objecto)
        assertEquals("UNICEF", c2.ugea)
        assertEquals("Moçambique", c2.provincia) // fallback de Multiple destinations
        assertEquals("UNGM", c2.fonte)
        assertFalse(c2.ehInformatica)
    }

    @Test
    fun parseDetalhes_extrai_campos_descricao_e_documentos() {
        val detalhes = UngmScraper.parseDetalhes(htmlDetalhes, "UNDP-MOZ-00123")
        assertEquals("UNDP-MOZ-00123", detalhes.referencia)
        assertEquals("https://www.ungm.org/files/rfp_undp_moz.pdf", detalhes.linkDocumento)

        val campoDesc = detalhes.campos.find { it.rotulo.equals("Descrição", ignoreCase = true) }
        assertTrue(campoDesc != null)
        assertTrue(campoDesc!!.valor.contains("datacenter"))

        val campoRef = detalhes.campos.find { it.rotulo.equals("Reference", ignoreCase = true) }
        assertTrue(campoRef != null)
        assertEquals("UNDP-MOZ-00123", campoRef!!.valor)
    }
}
