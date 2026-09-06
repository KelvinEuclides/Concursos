package mz.co.kevin.concursos.data.util

import java.text.Normalizer

/**
 * Correspondência tolerante entre nomes de documentos (ex.: "Alvará comercial"
 * vs. "Alvará / Licença Comercial válida"). Usado pela checklist de proposta
 * (#47) e pelo ecrã de prontidão (#49). Puro — testável em JVM.
 */
object DocumentoMatcher {

    /** Normaliza para minúsculas sem acentos nem pontuação, espaços colapsados. */
    fun normalizar(texto: String): String = Normalizer
        .normalize(texto.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    /** True se algum documento em [empresaDocs] corresponde a [exigido]. */
    fun corresponde(empresaDocs: List<String>, exigido: String): Boolean {
        val alvo = normalizar(exigido)
        if (alvo.isBlank()) return false
        val palavrasAlvo = alvo.split(" ").filter { it.length > 3 }.toSet()
        return empresaDocs.any { disp ->
            val d = normalizar(disp)
            if (d.isBlank()) return@any false
            if (d.contains(alvo) || alvo.contains(d)) return@any true
            val palavras = d.split(" ").filter { it.length > 3 }.toSet()
            palavrasAlvo.isNotEmpty() && palavras.isNotEmpty() &&
                palavrasAlvo.intersect(palavras).size >= 2
        }
    }
}
