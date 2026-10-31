package mz.co.kevin.concursos

import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.RecomendacaoConcursoIa
import mz.co.kevin.concursos.worker.DailySyncWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailySyncWorkerFiltroTest {

    private fun concurso(ref: String) = Concurso(
        referencia = ref,
        modalidade = "Concurso Público",
        objecto = "Objecto $ref",
        ugea = "UGEA",
        provincia = "Maputo",
        dataLancamento = "2026-01-01",
        dataAbertura = "2026-02-01",
        linkDetalhes = "",
        categoria = CategoriaConcurso.ABERTO,
        ehInformatica = false,
        fonte = "UFSA"
    )

    private fun rec(ref: String, nivel: String, score: Int) = RecomendacaoConcursoIa(
        referencia = ref,
        nivelCompatibilidade = nivel,
        scoreCompatibilidade = score,
        resumoAvaliacao = ""
    )

    @Test fun `nivel ALTA e incluido independentemente do score`() {
        val resultado = DailySyncWorker.filtrarAltaCompatibilidade(
            novos = listOf(concurso("REF1")),
            recomendacoes = listOf(rec("REF1", "ALTA", 50))
        )
        assertEquals(1, resultado.size)
        assertEquals("REF1", resultado[0].first.referencia)
    }

    @Test fun `score acima de SCORE_ALTA e incluido mesmo com nivel MEDIA`() {
        val resultado = DailySyncWorker.filtrarAltaCompatibilidade(
            novos = listOf(concurso("REF2")),
            recomendacoes = listOf(rec("REF2", "MEDIA", DailySyncWorker.SCORE_ALTA))
        )
        assertEquals(1, resultado.size)
    }

    @Test fun `nivel BAIXA com score abaixo do limiar e excluido`() {
        val resultado = DailySyncWorker.filtrarAltaCompatibilidade(
            novos = listOf(concurso("REF3")),
            recomendacoes = listOf(rec("REF3", "BAIXA", DailySyncWorker.SCORE_ALTA - 1))
        )
        assertTrue(resultado.isEmpty())
    }

    @Test fun `concurso sem recomendacao e excluido`() {
        val resultado = DailySyncWorker.filtrarAltaCompatibilidade(
            novos = listOf(concurso("REF4")),
            recomendacoes = listOf(rec("OUTRO", "ALTA", 90))
        )
        assertTrue(resultado.isEmpty())
    }

    @Test fun `lista vazia devolve vazio`() {
        assertTrue(
            DailySyncWorker.filtrarAltaCompatibilidade(emptyList(), emptyList()).isEmpty()
        )
    }

    @Test fun `ordem segue a lista de novos concursos`() {
        val novos = listOf(concurso("B"), concurso("A"))
        val recs = listOf(rec("A", "ALTA", 90), rec("B", "ALTA", 80))
        val resultado = DailySyncWorker.filtrarAltaCompatibilidade(novos, recs)
        assertEquals(listOf("B", "A"), resultado.map { it.first.referencia })
    }
}
