package mz.co.kevin.concursos.data.prontidao

import mz.co.kevin.concursos.data.model.ConcursoGuardado
import mz.co.kevin.concursos.data.model.RecomendacaoConcursoIa
import mz.co.kevin.concursos.data.util.DocumentoMatcher

/** Prontidão de um concurso guardado: documentos exigidos vs. os que a empresa tem. */
data class ProntidaoConcurso(
    val referencia: String,
    val objecto: String,
    val exigidos: List<String>,
    val emFalta: List<String>,
) {
    val possuidos: Int get() = exigidos.size - emFalta.size
    val percentagem: Int
        get() = if (exigidos.isEmpty()) 100 else (possuidos * 100) / exigidos.size
}

/** Um documento em falta e os concursos guardados que ele desbloqueia. */
data class DocumentoAObter(
    val documento: String,
    val concursos: List<ProntidaoConcurso>,
) {
    val cobertura: Int get() = concursos.size
}

data class ProntidaoResumo(
    val concursos: List<ProntidaoConcurso> = emptyList(),
    val documentosAObter: List<DocumentoAObter> = emptyList(),
) {
    val vazio: Boolean get() = concursos.isEmpty()
    val prontidaoMedia: Int
        get() = if (concursos.isEmpty()) 0 else concursos.map { it.percentagem }.average().toInt()
}

/**
 * Agrega, sem novas chamadas de IA (#49), as recomendações já guardadas sobre os
 * concursos que o utilizador guardou: percentagem de prontidão por concurso e a
 * lista global de documentos a obter, ordenada pelo nº de concursos que cada um
 * desbloqueia. Puro — testável em JVM.
 */
object ProntidaoCalculator {

    fun calcular(
        guardados: List<ConcursoGuardado>,
        recomendacoes: List<RecomendacaoConcursoIa>,
        documentosEmpresa: List<String>,
    ): ProntidaoResumo {
        val recPorRef = recomendacoes.associateBy { it.referencia }

        val concursos = guardados.mapNotNull { g ->
            val rec = recPorRef[g.referencia] ?: return@mapNotNull null

            val exigidos = rec.documentosExigidosProvaveis
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinctBy { it.lowercase() }

            val (exigidosFinal, emFalta) = if (exigidos.isNotEmpty()) {
                exigidos to exigidos.filterNot { DocumentoMatcher.corresponde(documentosEmpresa, it) }
            } else {
                // Sem lista de exigidos: usa os "em falta" que a IA devolveu.
                val ef = rec.documentosEmFalta.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
                ef to ef
            }

            if (exigidosFinal.isEmpty()) return@mapNotNull null

            ProntidaoConcurso(
                referencia = g.referencia,
                objecto = g.objecto,
                exigidos = exigidosFinal,
                emFalta = emFalta,
            )
        }.sortedBy { it.percentagem }

        // Documentos a obter: agrupa os "em falta" por nome normalizado.
        val agrupado = LinkedHashMap<String, MutableList<ProntidaoConcurso>>()
        val rotuloCanonico = HashMap<String, String>()
        for (c in concursos) {
            for (doc in c.emFalta) {
                val chave = DocumentoMatcher.normalizar(doc)
                if (chave.isEmpty()) continue
                rotuloCanonico.putIfAbsent(chave, doc)
                agrupado.getOrPut(chave) { mutableListOf() }.add(c)
            }
        }

        val documentosAObter = agrupado.entries
            .map { (chave, lista) -> DocumentoAObter(rotuloCanonico.getValue(chave), lista.toList()) }
            .sortedWith(compareByDescending<DocumentoAObter> { it.cobertura }.thenBy { it.documento.lowercase() })

        return ProntidaoResumo(concursos = concursos, documentosAObter = documentosAObter)
    }
}
