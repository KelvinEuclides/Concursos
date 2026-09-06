package mz.co.kevin.concursos.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Sistema de formas alinhado com a linguagem visual da agencia: cantos
 * generosamente arredondados, superficies suaves. Usado em toda a app via
 * `MaterialTheme.shapes` (chips, cartoes, campos, bottom sheets, botoes).
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
