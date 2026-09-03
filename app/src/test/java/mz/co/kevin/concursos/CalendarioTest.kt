package mz.co.kevin.concursos

import mz.co.kevin.concursos.ui.util.parseDataPortal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Testes puros (JVM) sobre [parseDataPortal] — a conversão de datas "yyyy-MM-dd"
 * do portal da UFSA para epoch millis. Não toca em nenhuma API do Android.
 */
class CalendarioTest {

    private fun esperado(data: String): Long =
        LocalDate.parse(data)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    @Test
    fun parseDataPortal_converte_data_valida() {
        assertEquals(esperado("2026-02-01"), parseDataPortal("2026-02-01"))
    }

    @Test
    fun parseDataPortal_ignora_espacos_em_volta() {
        assertEquals(esperado("2026-02-01"), parseDataPortal("  2026-02-01  "))
    }

    @Test
    fun parseDataPortal_usa_apenas_os_primeiros_10_caracteres() {
        // O portal às vezes anexa a hora — só a parte da data interessa.
        assertEquals(esperado("2026-02-01"), parseDataPortal("2026-02-01 14:30:00"))
        assertEquals(esperado("2026-02-01"), parseDataPortal("2026-02-01T00:00:00Z"))
    }

    @Test
    fun parseDataPortal_meia_noite_local() {
        val millis = parseDataPortal("2026-09-03")!!
        val reconstruida = java.time.Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        assertEquals(LocalDate.of(2026, 9, 3), reconstruida)
    }

    @Test
    fun parseDataPortal_devolve_null_em_entrada_invalida() {
        assertNull(parseDataPortal(""))
        assertNull(parseDataPortal("   "))
        assertNull(parseDataPortal("data desconhecida"))
        assertNull(parseDataPortal("01/02/2026"))
        assertNull(parseDataPortal("2026-13-40"))
    }
}
