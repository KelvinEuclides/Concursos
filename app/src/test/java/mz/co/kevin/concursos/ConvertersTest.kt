package mz.co.kevin.concursos

import mz.co.kevin.concursos.data.local.Converters
import mz.co.kevin.concursos.data.model.CategoriaConcurso
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testes puros (JVM) do [Converters] do Room — a serialização da enum
 * [CategoriaConcurso] para texto e a leitura de volta.
 */
class ConvertersTest {

    private val converters = Converters()

    @Test
    fun categoriaParaTexto_usa_o_name_da_enum() {
        assertEquals("ABERTO", converters.categoriaParaTexto(CategoriaConcurso.ABERTO))
        assertEquals("ADJUDICADO", converters.categoriaParaTexto(CategoriaConcurso.ADJUDICADO))
        assertEquals("CANCELADO", converters.categoriaParaTexto(CategoriaConcurso.CANCELADO))
    }

    @Test
    fun textoParaCategoria_reconstroi_a_enum() {
        assertEquals(CategoriaConcurso.ABERTO, converters.textoParaCategoria("ABERTO"))
        assertEquals(CategoriaConcurso.CANCELADO, converters.textoParaCategoria("CANCELADO"))
    }

    @Test
    fun roundTrip_preserva_todos_os_valores() {
        for (cat in CategoriaConcurso.entries) {
            val restaurada = converters.textoParaCategoria(converters.categoriaParaTexto(cat))
            assertEquals(cat, restaurada)
        }
    }

    @Test
    fun textoParaCategoria_cai_em_ABERTO_para_valor_desconhecido() {
        assertEquals(CategoriaConcurso.ABERTO, converters.textoParaCategoria("INEXISTENTE"))
        assertEquals(CategoriaConcurso.ABERTO, converters.textoParaCategoria(""))
        assertEquals(CategoriaConcurso.ABERTO, converters.textoParaCategoria("aberto"))
    }
}
