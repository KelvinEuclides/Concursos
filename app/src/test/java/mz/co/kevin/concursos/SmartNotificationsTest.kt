package mz.co.kevin.concursos

import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.RecomendacaoConcursoIa
import mz.co.kevin.concursos.worker.DailySyncWorker
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Feature #46: só notificar concursos de alta compatibilidade.
 * Testa o cruzamento puro [DailySyncWorker.filtrarAltaCompatibilidade].
 */
class SmartNotificationsTest {

    private fun concurso(ref: String) = Concurso(
        referencia = ref,
        modalidade = "Concurso Público",
        objecto = "Objecto $ref",
        ugea = "UGEA $ref",
        provincia = "Maputo Cidade",
        dataLancamento = "2026-02-01",
        dataAbertura = "2026-02-20",
        linkDetalhes = "",
        categoria = CategoriaConcurso.ABERTO,
        ehInformatica = false,
    )

    private fun rec(ref: String, score: Int, nivel: String) = RecomendacaoConcursoIa(
        referencia = ref,
        scoreCompatibilidade = score,
        nivelCompatibilidade = nivel,
        resumoAvaliacao = "",
    )

    @Test
    fun mantem_apenas_alta_por_nivel_ou_score() {
        val novos = listOf(concurso("A"), concurso("B"), concurso("C"), concurso("D"))
        val recs = listOf(
            rec("A", 85, "ALTA"),
            rec("B", 40, "BAIXA"),
            rec("C", 72, "MEDIA"), // score >= 70 => conta
            rec("D", 69, "MEDIA"),
        )

        val altos = DailySyncWorker.filtrarAltaCompatibilidade(novos, recs)

        assertEquals(listOf("A", "C"), altos.map { it.first.referencia })
    }

    @Test
    fun concurso_sem_recomendacao_e_ignorado() {
        val novos = listOf(concurso("A"), concurso("B"))
        val recs = listOf(rec("A", 90, "ALTA")) // B não foi avaliado

        val altos = DailySyncWorker.filtrarAltaCompatibilidade(novos, recs)

        assertEquals(listOf("A"), altos.map { it.first.referencia })
    }

    @Test
    fun preserva_a_ordem_dos_concursos_novos() {
        val novos = listOf(concurso("X"), concurso("Y"), concurso("Z"))
        val recs = listOf(
            rec("Z", 95, "ALTA"),
            rec("X", 80, "ALTA"),
            rec("Y", 75, "ALTA"),
        )

        val altos = DailySyncWorker.filtrarAltaCompatibilidade(novos, recs)

        assertEquals(listOf("X", "Y", "Z"), altos.map { it.first.referencia })
    }

    @Test
    fun nada_de_alta_devolve_lista_vazia() {
        val novos = listOf(concurso("A"), concurso("B"))
        val recs = listOf(rec("A", 30, "BAIXA"), rec("B", 55, "MEDIA"))

        assertEquals(emptyList<String>(), DailySyncWorker.filtrarAltaCompatibilidade(novos, recs).map { it.first.referencia })
    }
}
