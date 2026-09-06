package mz.co.kevin.concursos

import mz.co.kevin.concursos.ui.icons.AppIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sanidade do conjunto de ícones Font Awesome ([AppIcon]): cada entrada tem de
 * ter um code point plausível e produzir um glifo de um caractere. Se um code
 * point sair da zona de uso privado, provavelmente está errado.
 */
class AppIconTest {

    @Test
    fun todos_os_code_points_estao_na_zona_de_uso_privado_do_fa() {
        AppIcon.entries.forEach { icon ->
            assertTrue(
                "${icon.name} (0x${icon.code.toString(16)}) fora do intervalo esperado",
                icon.code in 0xE000..0xF8FF,
            )
        }
    }

    @Test
    fun cada_icone_produz_um_glifo_de_um_caractere() {
        AppIcon.entries.forEach { icon ->
            assertEquals(icon.name, 1, icon.glyph.length)
            assertEquals(icon.code, icon.glyph.single().code)
        }
    }

    @Test
    fun ha_pelo_menos_um_icone_de_cada_peso() {
        assertTrue(AppIcon.entries.any { it.regular })
        assertTrue(AppIcon.entries.any { !it.regular })
    }
}
