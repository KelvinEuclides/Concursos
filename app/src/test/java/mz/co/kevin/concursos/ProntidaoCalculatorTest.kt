package mz.co.kevin.concursos

import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.model.ConcursoGuardado
import mz.co.kevin.concursos.data.model.RecomendacaoConcursoIa
import mz.co.kevin.concursos.data.prontidao.ProntidaoCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProntidaoCalculatorTest {

    private fun guardado(ref: String, objecto: String = "Objecto $ref") = ConcursoGuardado(
        referencia = ref,
        modalidade = "Concurso Público",
        objecto = objecto,
        ugea = "UGEA",
        provincia = "Maputo Cidade",
        categoria = CategoriaConcurso.ABERTO,
        ehInformatica = false,
        dataInicioSubmissao = "",
        dataFimSubmissao = "",
        linkDetalhes = "",
    )

    private fun rec(ref: String, exigidos: List<String>) = RecomendacaoConcursoIa(
        referencia = ref,
        scoreCompatibilidade = 50,
        nivelCompatibilidade = "MEDIA",
        resumoAvaliacao = "",
        documentosExigidosProvaveis = exigidos,
    )

    private val docsEmpresa = listOf("Alvará / Licença Comercial válida", "Certidão de Quitação do INSS")

    @Test
    fun percentagem_por_concurso_e_media() {
        val guardados = listOf(guardado("A"), guardado("B"))
        val recs = listOf(
            rec("A", listOf("Alvará comercial", "Certidão do INSS")),          // 2/2 = 100
            rec("B", listOf("Alvará comercial", "Garantia provisória", "Balanço auditado")), // 1/3 = 33
        )

        val r = ProntidaoCalculator.calcular(guardados, recs, docsEmpresa)

        val porRef = r.concursos.associateBy { it.referencia }
        assertEquals(100, porRef.getValue("A").percentagem)
        assertEquals(33, porRef.getValue("B").percentagem)
        assertEquals(66, r.prontidaoMedia) // (100 + 33) / 2 = 66 (trunc)
        // ordenado por prontidão ascendente
        assertEquals(listOf("B", "A"), r.concursos.map { it.referencia })
    }

    @Test
    fun concurso_guardado_sem_recomendacao_e_ignorado() {
        val r = ProntidaoCalculator.calcular(
            listOf(guardado("A"), guardado("SEM_REC")),
            listOf(rec("A", listOf("Alvará comercial"))),
            docsEmpresa,
        )
        assertEquals(listOf("A"), r.concursos.map { it.referencia })
    }

    @Test
    fun documentos_a_obter_agrega_e_ordena_por_cobertura() {
        val guardados = listOf(guardado("A"), guardado("B"), guardado("C"))
        val recs = listOf(
            rec("A", listOf("Garantia provisória", "Balanço auditado")),
            rec("B", listOf("Garantia provisoria")), // mesma coisa, sem acento
            rec("C", listOf("Garantia provisória")),
        )

        val r = ProntidaoCalculator.calcular(guardados, recs, docsEmpresa)

        assertEquals("Garantia provisória", r.documentosAObter.first().documento)
        assertEquals(3, r.documentosAObter.first().cobertura)
        assertTrue(r.documentosAObter.any { it.documento == "Balanço auditado" && it.cobertura == 1 })
    }

    @Test
    fun vazio_quando_nao_ha_recomendacoes_para_guardados() {
        val r = ProntidaoCalculator.calcular(listOf(guardado("A")), emptyList(), docsEmpresa)
        assertTrue(r.vazio)
        assertEquals(0, r.prontidaoMedia)
    }
}
