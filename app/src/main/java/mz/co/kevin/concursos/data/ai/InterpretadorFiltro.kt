package mz.co.kevin.concursos.data.ai

import mz.co.kevin.concursos.data.model.FiltroConcursos
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Parser de regras (offline, puro) para o filtro em linguagem natural — feature
 * #48. Sem IA: reconhece províncias conhecidas, "TI", e prazos relativos comuns
 * em PT/EN. O que sobrar vira termo de pesquisa.
 */
object InterpretadorFiltro {

    fun local(
        texto: String,
        provincias: List<String>,
        categorias: List<String>,
        hoje: LocalDate = LocalDate.now(),
    ): FiltroConcursos {
        val t = " ${texto.lowercase().trim()} "
        if (t.isBlank()) return FiltroConcursos()

        val provincia = provincias.firstOrNull { p ->
            p.isNotBlank() && !p.equals("Todas as Províncias", true) && t.contains(p.lowercase())
        } ?: PROVINCIAS_CURTAS.entries
            .firstOrNull { t.contains(" ${it.key} ") || t.contains(" ${it.key},") }
            ?.value
            ?.let { canonico -> provincias.firstOrNull { it.equals(canonico, true) } ?: canonico }

        val apenasTi = listOf(" ti ", " t.i ", "informát", "informat", " it ", "tecnolog")
            .any { t.contains(it) }

        val categoria = categorias.firstOrNull { c -> t.contains(c.lowercase()) }

        val prazo = when {
            listOf("hoje", "today").any { t.contains(it) } -> hoje
            listOf("amanhã", "amanha", "tomorrow").any { t.contains(it) } -> hoje.plusDays(1)
            listOf("esta semana", "this week").any { t.contains(it) } ->
                hoje.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            listOf("este mês", "este mes", "this month").any { t.contains(it) } ->
                hoje.with(TemporalAdjusters.lastDayOfMonth())
            listOf("próxima semana", "proxima semana", "next week").any { t.contains(it) } ->
                hoje.plusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            else -> Regex("(?:em|in|nos próximos|nos proximos|next)\\s+(\\d{1,3})\\s*(dias?|days?)")
                .find(t)?.groupValues?.get(1)?.toIntOrNull()?.let { hoje.plusDays(it.toLong()) }
        }

        // Termo: remove tokens já consumidos e stopwords.
        val consumidas = buildSet {
            provincia?.let { addAll(it.lowercase().split(" ")) }
            categoria?.let { addAll(it.lowercase().split(" ")) }
            addAll(STOPWORDS)
            if (apenasTi) addAll(listOf("ti", "it", "informática", "informatica", "tecnologia"))
            if (prazo != null) addAll(listOf("hoje", "today", "amanhã", "amanha", "tomorrow", "semana", "week", "mês", "mes", "month", "dias", "days", "próxima", "proxima", "próximos", "proximos", "next", "this", "esta", "este", "em", "in", "nos"))
        }
        val termo = t.trim().split(Regex("\\s+"))
            .filter { it.length > 2 && it !in consumidas && it.none { ch -> ch.isDigit() } }
            .joinToString(" ")
            .ifBlank { null }

        return FiltroConcursos(
            provincia = provincia,
            categoriaIa = categoria,
            apenasTi = apenasTi,
            prazoAntesDe = prazo?.toString(),
            termo = termo,
        )
    }

    private val STOPWORDS = setOf(
        "concurso", "concursos", "tender", "tenders", "que", "de", "do", "da", "em", "no", "na",
        "os", "as", "com", "para", "the", "in", "on", "at", "and", "or", "por", "mostra", "show",
        "quero", "want", "ver", "filtrar", "filter", "abertos", "open", "fecham", "close", "closing",
        "acima", "over", "mais", "more",
    )

    // atalhos p/ nomes parciais → nome canónico esperado na lista de províncias
    private val PROVINCIAS_CURTAS = mapOf(
        "maputo" to "Maputo Cidade",
        "cidade" to "Maputo Cidade",
        "nampula" to "Nampula",
        "beira" to "Sofala",
        "sofala" to "Sofala",
        "gaza" to "Gaza",
        "tete" to "Tete",
        "manica" to "Manica",
        "niassa" to "Niassa",
        "zambezia" to "Zambézia",
        "inhambane" to "Inhambane",
    )

}
