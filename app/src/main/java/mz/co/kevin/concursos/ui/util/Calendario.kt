package mz.co.kevin.concursos.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.core.net.toUri
import java.time.LocalDate
import java.time.ZoneId

/** Converte "yyyy-MM-dd" (formato do portal UFSA) em epoch millis, ou null. */
fun parseDataPortal(valor: String): Long? = runCatching {
    val limpo = valor.trim().take(10)
    LocalDate.parse(limpo)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}.getOrNull()

/**
 * Abre o calendário do sistema (ACTION_INSERT — não requer permissões) já
 * preenchido com o período de submissão, o link e os requisitos do concurso.
 */
fun Context.adicionarConcursoAoCalendario(
    titulo: String,
    inicioSubmissao: String,
    fimSubmissao: String,
    link: String,
    requisitos: String
) {
    val inicioMillis = parseDataPortal(inicioSubmissao)
    val fimMillis = parseDataPortal(fimSubmissao)

    val descricao = buildString {
        if (requisitos.isNotBlank()) {
            appendLine("Requisitos / detalhes:")
            appendLine(requisitos)
            appendLine()
        }
        if (link.isNotBlank()) appendLine("Link: $link")
        if (inicioSubmissao.isNotBlank()) appendLine("Início de submissão: $inicioSubmissao")
        if (fimSubmissao.isNotBlank()) appendLine("Fim de submissão: $fimSubmissao")
    }.trim()

    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, "Submissão: $titulo")
        putExtra(CalendarContract.Events.DESCRIPTION, descricao)
        putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        if (inicioMillis != null) putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, inicioMillis)
        // O fim de submissão é a data-limite: marca o evento a terminar nesse dia.
        if (fimMillis != null) putExtra(CalendarContract.EXTRA_EVENT_END_TIME, fimMillis)
    }

    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "Nenhuma aplicação de calendário encontrada", Toast.LENGTH_SHORT).show()
    }
}

fun Context.abrirUrl(url: String) {
    if (url.isBlank()) return
    try {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "Não foi possível abrir o link", Toast.LENGTH_SHORT).show()
    }
}
