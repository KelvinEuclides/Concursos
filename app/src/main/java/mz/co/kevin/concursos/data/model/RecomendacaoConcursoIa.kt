package mz.co.kevin.concursos.data.model

/**
 * Resultado da avaliação de um concurso gerado pelo Google AI (Gemini)
 * para o perfil e documentos de uma empresa específica.
 */
data class RecomendacaoConcursoIa(
    val referencia: String,
    val scoreCompatibilidade: Int, // 0 a 100
    val nivelCompatibilidade: String, // "ALTA", "MEDIA", "BAIXA"
    val resumoAvaliacao: String,
    val pontosFortes: List<String> = emptyList(),
    val documentosExigidosProvaveis: List<String> = emptyList(),
    val documentosEmFalta: List<String> = emptyList(),
    val recomendacaoEstrategica: String = ""
)
