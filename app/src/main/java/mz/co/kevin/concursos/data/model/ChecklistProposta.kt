package mz.co.kevin.concursos.data.model

/**
 * Um item da checklist de submissão. Para documentos, [disponivel] diz se a
 * empresa declarou tê-lo no perfil; [concluido] é o estado marcável pelo
 * utilizador. Feature #47.
 */
data class ItemChecklist(
    val texto: String,
    val disponivel: Boolean = false,
    val concluido: Boolean = false,
)

/** Uma secção do esqueleto da proposta com os pontos a incluir. */
data class SeccaoProposta(
    val titulo: String,
    val pontos: List<String> = emptyList(),
)

/**
 * Plano accionável para preparar a proposta de um concurso: checklist de
 * documentos (exigidos vs. disponíveis), formato/datas de entrega e um
 * esqueleto de secções. Persistido por referência em [mz.co.kevin.concursos.data.repository.PerfilEmpresaRepository].
 */
data class ChecklistProposta(
    val referencia: String,
    val documentos: List<ItemChecklist> = emptyList(),
    val formatoEntrega: String = "",
    val datasChave: List<String> = emptyList(),
    val esqueleto: List<SeccaoProposta> = emptyList(),
) {
    val totalDocumentos: Int get() = documentos.size
    val documentosConcluidos: Int get() = documentos.count { it.concluido }
    val documentosEmFalta: Int get() = documentos.count { !it.disponivel }
}
