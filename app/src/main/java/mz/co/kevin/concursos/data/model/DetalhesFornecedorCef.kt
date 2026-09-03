package mz.co.kevin.concursos.data.model

/**
 * Resultado do scraping de `inscritoscef_detalhes.php`. Estrutura transitória
 * (não persistida): é buscada sob demanda quando o utilizador abre a ficha de
 * um fornecedor. Os campos são pares rótulo/valor genéricos porque a página do
 * portal agrupa ramos de actividade, contactos e regime numa ou mais tabelas
 * cuja estrutura exacta pode variar.
 */
data class DetalhesFornecedorCef(
    val certificado: String,
    val campos: List<CampoDetalhe>
) {
    /** Concatena todos os valores de campos cujo rótulo contenha [chaves]. */
    fun valoresDe(vararg chaves: String): String =
        campos.filter { c -> chaves.any { c.rotulo.contains(it, ignoreCase = true) } }
            .joinToString("\n") { it.valor }
}
