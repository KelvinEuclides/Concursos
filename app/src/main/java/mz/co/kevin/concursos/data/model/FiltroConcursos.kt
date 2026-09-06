package mz.co.kevin.concursos.data.model

/**
 * Resultado da interpretação de um filtro escrito em linguagem natural
 * (feature #48). Campos a `null` / `false` = "não mencionado".
 */
data class FiltroConcursos(
    val provincia: String? = null,
    val categoriaIa: String? = null,
    val apenasTi: Boolean = false,
    /** Data-limite ISO `yyyy-MM-dd`; concursos que abrem até esta data. */
    val prazoAntesDe: String? = null,
    val termo: String? = null,
) {
    val vazio: Boolean
        get() = provincia == null && categoriaIa == null && !apenasTi &&
            prazoAntesDe == null && termo.isNullOrBlank()
}
