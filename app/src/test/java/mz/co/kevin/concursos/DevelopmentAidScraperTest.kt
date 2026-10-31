package mz.co.kevin.concursos

import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.remote.DevelopmentAidScraper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Testes unitários puros (JVM) para o [DevelopmentAidScraper], validando o parsing
 * de editais de doadores internacionais em Moçambique e a detecção de desafios de rede/Cloudflare.
 */
class DevelopmentAidScraperTest {

    private val htmlTenders = """
        <div class="search-results">
            <div class="tender-card" data-tender-id="1045231">
                <span class="reference">Ref: DEV-WB-P178234-MOZ</span>
                <h3 class="title">
                    <a href="/tenders/1045231/supply-of-it-and-networking-hardware-mozambique">
                        Supply of IT and networking hardware for Mozambique Municipalities
                    </a>
                </h3>
                <span class="agency">World Bank</span>
                <span class="type">Goods</span>
                <span class="location">Moçambique</span>
                <span class="deadline">Deadline: 2026-12-15</span>
                <span class="posted-date">Posted: 2026-10-01</span>
            </div>
            <div class="tender-card" data-tender-id="1045232">
                <h3 class="title">
                    <a href="/tenders/1045232/rural-sanitation-project">
                        Rural Sanitation and Water Supply Project
                    </a>
                </h3>
                <span class="agency">African Development Bank</span>
                <span class="type">Consulting Services</span>
                <span class="location">Zambézia</span>
                <span class="deadline">Deadline: 2026-11-20</span>
                <span class="posted-date">Posted: 2026-09-15</span>
            </div>
        </div>
    """.trimIndent()

    private val jsonProcurement = """
        {
          "procnotices": [
            {
              "id": "OP00477484",
              "notice_type": "Request for Expression of Interest",
              "noticedate": "27-Oct-2026",
              "submission_deadline_date": "2026-11-16T00:00:00Z",
              "bid_reference_no": "MZ-MEF-DNT-577804-CS-QCBS",
              "bid_description": "Consultancy Services for Master Plan",
              "project_name": "Mais Oportunidades Moçambique"
            },
            {
              "id": "OP00477371",
              "notice_type": "Contract Award",
              "noticedate": "27-Oct-2026",
              "bid_reference_no": "46/CONS/ILEGP/22",
              "bid_description": "Consulting service for books TIC",
              "project_name": "Empowering Girls in Mozambique"
            }
          ]
        }
    """.trimIndent()

    private val htmlDetalhes = """
        <div class="tender-details">
            <div class="tender-description">
                Procurement of switches, routers and optical fiber equipment for provincial offices in Mozambique.
            </div>
            <div class="meta-item">
                <span class="label">Donor:</span>
                <span class="value">World Bank</span>
            </div>
            <div class="meta-item">
                <span class="label">Location:</span>
                <span class="value">Maputo, Moçambique</span>
            </div>
            <a class="btn-download" href="https://www.developmentaid.org/docs/wb_rfp_moz.pdf">Baixar Documento</a>
        </div>
    """.trimIndent()

    @Test
    fun parseConcursos_extrai_tenders_developmentaid() {
        val concursos = DevelopmentAidScraper.parseConcursos(htmlTenders, CategoriaConcurso.ABERTO)
        assertEquals(2, concursos.size)

        val c1 = concursos[0]
        assertEquals("DEV-WB-P178234-MOZ", c1.referencia)
        assertEquals("Supply of IT and networking hardware for Mozambique Municipalities", c1.objecto)
        assertEquals("World Bank", c1.ugea)
        assertEquals("Goods", c1.modalidade)
        assertEquals("Moçambique", c1.provincia)
        assertEquals("2026-12-15", c1.dataAbertura)
        assertEquals("2026-10-01", c1.dataLancamento)
        assertEquals("https://www.developmentaid.org/tenders/1045231/supply-of-it-and-networking-hardware-mozambique", c1.linkDetalhes)
        assertEquals("DevelopmentAid", c1.fonte)
        assertTrue(c1.ehInformatica) // networking / IT

        val c2 = concursos[1]
        assertEquals("DEV-1045232", c2.referencia) // gerado a partir do data-tender-id
        assertEquals("African Development Bank", c2.ugea)
        assertEquals("Zambézia", c2.provincia)
        assertEquals("DevelopmentAid", c2.fonte)
        assertFalse(c2.ehInformatica)
    }

    @Test
    fun parseProcurementJson_extrai_editais_de_doadores_para_mocambique() {
        val abertos = DevelopmentAidScraper.parseProcurementJson(jsonProcurement, CategoriaConcurso.ABERTO)
        assertEquals(1, abertos.size)
        val a1 = abertos[0]
        assertEquals("DEV-MZ-MEF-DNT-577804-CS-QCBS", a1.referencia)
        assertEquals("Consultancy Services for Master Plan", a1.objecto)
        assertEquals("2026-11-16", a1.dataAbertura)
        assertEquals("DevelopmentAid", a1.fonte)
        assertEquals(CategoriaConcurso.ABERTO, a1.categoria)

        val adjudicados = DevelopmentAidScraper.parseProcurementJson(jsonProcurement, CategoriaConcurso.ADJUDICADO)
        assertEquals(1, adjudicados.size)
        val adj1 = adjudicados[0]
        assertEquals("DEV-46/CONS/ILEGP/22", adj1.referencia)
        assertEquals("DevelopmentAid", adj1.fonte)
        assertEquals(CategoriaConcurso.ADJUDICADO, adj1.categoria)
        assertTrue(adj1.ehInformatica) // TIC
    }

    @Test
    fun parseDetalhes_extrai_campos_e_documentos() {
        val detalhes = DevelopmentAidScraper.parseDetalhes(htmlDetalhes, "DEV-1045231")
        assertEquals("DEV-1045231", detalhes.referencia)
        assertEquals("https://www.developmentaid.org/docs/wb_rfp_moz.pdf", detalhes.linkDocumento)

        val desc = detalhes.campos.find { it.rotulo == "Descrição" }
        assertTrue(desc != null)
        assertTrue(desc!!.valor.contains("switches, routers"))

        val donor = detalhes.campos.find { it.rotulo == "Donor" }
        assertTrue(donor != null)
        assertEquals("World Bank", donor!!.valor)
    }

    @Test
    fun validarResposta_detecta_desafio_cloudflare() {
        assertThrows(IOException::class.java) {
            DevelopmentAidScraper.validarResposta("<!DOCTYPE html><html><title>Just a moment...</title>challenges.cloudflare.com</html>", 403)
        }
    }
}
