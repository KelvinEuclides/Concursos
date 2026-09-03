package mz.co.kevin.concursos

import mz.co.kevin.concursos.ui.screens.anoDaData
import org.junit.Assert.assertEquals
import org.junit.Test

/** Testa a extração do ano usada pelo filtro de fornecedores (JVM puro). */
class AnoDaDataTest {

    @Test
    fun extrai_ano_de_formato_portal_dd_mm_yyyy() {
        assertEquals("2020", anoDaData("19/11/2020"))
        assertEquals("2024", anoDaData("16/08/2024"))
    }

    @Test
    fun extrai_ano_de_formato_iso_yyyy_mm_dd() {
        assertEquals("2025", anoDaData("2025-03-01"))
    }

    @Test
    fun devolve_vazio_quando_nao_ha_ano() {
        assertEquals("", anoDaData(""))
        assertEquals("", anoDaData("sem data"))
    }
}
