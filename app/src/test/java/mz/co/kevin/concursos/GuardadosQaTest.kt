package mz.co.kevin.concursos

import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.data.ai.GoogleAiService
import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.model.ConcursoGuardado
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Heurística offline de "perguntar aos guardados" ([GoogleAiService.gerarRespostaLocalGuardados]).
 * JVM puro, com um resolvedor de strings mínimo.
 */
class GuardadosQaTest {

    private val service = GoogleAiService(resolveString = ::resolver)

    private fun guardado(
        ref: String,
        objecto: String = "Objecto $ref",
        prazo: String = "",
        requisitos: String = "",
    ) = ConcursoGuardado(
        referencia = ref,
        modalidade = "Concurso Público",
        objecto = objecto,
        ugea = "UGEA",
        provincia = "Maputo",
        categoria = CategoriaConcurso.ABERTO,
        ehInformatica = false,
        dataInicioSubmissao = "",
        dataFimSubmissao = prazo,
        linkDetalhes = "",
        requisitos = requisitos,
    )

    @Test
    fun pergunta_de_prazo_ordena_pelo_fim_de_submissao() {
        val lista = listOf(
            guardado("C", prazo = "2026-05-10"),
            guardado("A", prazo = "2026-01-02"),
            guardado("B", prazo = "2026-03-01"),
        )
        val r = service.gerarRespostaLocalGuardados(lista, "Quais fecham primeiro?")
        val ordem = listOf("A", "B", "C").map { r.indexOf("• $it ") }
        assertTrue("esperado A<B<C na resposta:\n$r", ordem[0] < ordem[1] && ordem[1] < ordem[2])
    }

    @Test
    fun pergunta_de_documento_filtra_pelos_requisitos() {
        val lista = listOf(
            guardado("A", requisitos = "Alvará, Certidão do INSS, NUIT"),
            guardado("B", requisitos = "Apenas registo comercial"),
        )
        val r = service.gerarRespostaLocalGuardados(lista, "Quais exigem certidão do INSS?")
        assertTrue(r.contains("• A "))
        assertTrue(!r.contains("• B "))
    }

    @Test
    fun pergunta_generica_lista_todos() {
        val lista = listOf(guardado("A"), guardado("B"))
        val r = service.gerarRespostaLocalGuardados(lista, "resume os meus guardados")
        assertTrue(r.contains("• A "))
        assertTrue(r.contains("• B "))
    }
}

private fun resolver(resId: Int, args: Array<out Any?>): String = when (resId) {
    R.string.guardados_qa_local_prazos -> "Prazos:"
    R.string.guardados_qa_local_docs -> "Docs \"${args.getOrNull(0)}\":"
    R.string.guardados_qa_local_geral -> "Tens ${args.getOrNull(0)} guardado(s):"
    R.string.guardados_qa_local_nenhum -> "Nenhum."
    else -> "res:$resId"
}
