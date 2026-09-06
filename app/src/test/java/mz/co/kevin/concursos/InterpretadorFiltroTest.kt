package mz.co.kevin.concursos

import mz.co.kevin.concursos.data.ai.InterpretadorFiltro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Parser de regras offline do filtro em linguagem natural (feature #48).
 * JVM puro — sem IA, sem Android.
 */
class InterpretadorFiltroTest {

    private val provincias = listOf("Maputo Cidade", "Nampula", "Sofala", "Gaza")
    private val categorias = listOf("Obras", "Bens", "Serviços")
    private val hoje = LocalDate.of(2026, 2, 10) // uma terça-feira

    private fun interpretar(texto: String) =
        InterpretadorFiltro.local(texto, provincias, categorias, hoje)

    @Test
    fun texto_vazio_devolve_filtro_vazio() {
        assertTrue(interpretar("   ").vazio)
    }

    @Test
    fun reconhece_provincia_pelo_nome_completo() {
        assertEquals("Nampula", interpretar("concursos em Nampula").provincia)
    }

    @Test
    fun reconhece_provincia_por_atalho() {
        assertEquals("Maputo Cidade", interpretar("tenders in Maputo").provincia)
        assertEquals("Sofala", interpretar("concursos na Beira").provincia)
    }

    @Test
    fun deteta_apenas_ti() {
        assertTrue(interpretar("concursos de TI abertos").apenasTi)
        assertTrue(interpretar("area de informática").apenasTi)
        assertFalse(interpretar("concursos de obras").apenasTi)
    }

    @Test
    fun reconhece_categoria_da_lista() {
        assertEquals("Obras", interpretar("mostrar concursos de Obras").categoriaIa)
    }

    @Test
    fun prazo_hoje_e_amanha() {
        assertEquals(hoje.toString(), interpretar("concursos que fecham hoje").prazoAntesDe)
        assertEquals(hoje.plusDays(1).toString(), interpretar("closing tomorrow").prazoAntesDe)
    }

    @Test
    fun prazo_esta_semana_vai_ate_domingo() {
        val domingo = hoje.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        assertEquals(domingo.toString(), interpretar("concursos esta semana").prazoAntesDe)
    }

    @Test
    fun prazo_este_mes_vai_ate_fim_do_mes() {
        assertEquals("2026-02-28", interpretar("tenders this month").prazoAntesDe)
    }

    @Test
    fun prazo_em_n_dias() {
        assertEquals(hoje.plusDays(7).toString(), interpretar("concursos em 7 dias").prazoAntesDe)
    }

    @Test
    fun palavras_livres_viram_termo() {
        val f = interpretar("concursos de canalização em Nampula")
        assertEquals("Nampula", f.provincia)
        assertTrue(f.termo!!.contains("canalização"))
    }

    @Test
    fun tokens_consumidos_nao_aparecem_no_termo() {
        val f = interpretar("concursos de TI em Maputo hoje")
        assertEquals("Maputo Cidade", f.provincia)
        assertTrue(f.apenasTi)
        assertEquals(hoje.toString(), f.prazoAntesDe)
        assertNull(f.termo)
    }

    @Test
    fun frase_combinada_completa() {
        val f = interpretar("quero ver concursos de Obras em Gaza que fecham este mês")
        assertEquals("Gaza", f.provincia)
        assertEquals("Obras", f.categoriaIa)
        assertEquals("2026-02-28", f.prazoAntesDe)
    }
}
