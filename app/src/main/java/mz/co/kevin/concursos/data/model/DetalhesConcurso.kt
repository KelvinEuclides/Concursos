package mz.co.kevin.concursos.data.model

/** Um par rótulo/valor extraído da tabela de detalhes do concurso. */
data class CampoDetalhe(val rotulo: String, val valor: String)

/**
 * Resultado do scraping de `concurso_detalhes.php`. Estrutura transitória
 * (não persistida): é buscada sob demanda quando o utilizador abre um concurso.
 */
data class DetalhesConcurso(
    val referencia: String,
    val campos: List<CampoDetalhe>,
    val linkAnuncio: String,
    val linkDocumento: String
)
