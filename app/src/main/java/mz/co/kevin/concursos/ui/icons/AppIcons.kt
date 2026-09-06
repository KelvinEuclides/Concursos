package mz.co.kevin.concursos.ui.icons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mz.co.kevin.concursos.R

/**
 * Icones da app em Font Awesome 6 Free.
 *
 * O estilo "line" (fa-light) so existe no Font Awesome Pro. Com o Free usamos o
 * traco "Regular" sempre que o glifo existe nesse peso e caimos para "Solid" nos
 * restantes (a maioria dos icones de accao). [code] e o code point do FA 6.
 */
private val FaRegular = FontFamily(Font(R.font.fa_regular_400, FontWeight.Normal))
private val FaSolid = FontFamily(Font(R.font.fa_solid_900, FontWeight.Black))

enum class AppIcon(val code: Int, val regular: Boolean) {
    // Accao / navegacao
    PESQUISAR(0xf002, false),        // magnifying-glass
    LIMPAR(0xf00d, false),           // xmark
    FILTRO(0xf0b0, false),           // filter
    SETA_DIREITA(0xf061, false),     // arrow-right
    CHEVRON_DIREITA(0xf054, false),  // chevron-right
    VOLTAR(0xf060, false),           // arrow-left

    // Navegacao principal / seccoes
    CONCURSOS(0xf1ea, true),         // newspaper
    FORNECEDORES(0xf1ad, true),      // building
    PRONTIDAO(0xf46c, false),        // clipboard-check
    DEFINICOES(0xf013, false),       // gear
    IA(0xf72b, false),               // wand-magic-sparkles

    // Cartoes de concurso
    GUARDAR(0xf02e, true),           // bookmark
    CALENDARIO(0xf133, true),        // calendar
    PERGUNTA(0xf059, true),          // circle-question
    COPIAR(0xf0c5, true),            // copy
    TI(0xf6ff, false),               // network-wired

    // Cartoes de fornecedor
    CERTIFICADO(0xf058, true),       // circle-check
    LOCALIZACAO(0xf3c5, false),      // location-dot
    CONTACTO(0xf095, false),         // phone

    // Estados / info
    INFO(0xf05a, false),             // circle-info
    AVISO(0xf071, false),            // triangle-exclamation
    LOJA(0xf54e, false),             // store

    // Definicoes / IA local
    DOCUMENTO(0xf15c, true),         // file-lines
    LEGAL(0xf0e3, false),            // gavel
    SEGURANCA(0xf3ed, false),        // shield-halved
    CHAVE(0xf084, false),            // key
    DESCARREGAR(0xf019, false),      // download
    IMPORTAR(0xf574, false),         // file-import
    ELIMINAR(0xf2ed, true),          // trash-can
    TESTAR(0xf04b, false),           // play
    OK(0xf058, true),                // circle-check
    OCULTAR(0xf070, true),           // eye-slash
    VER(0xf06e, true),               // eye
    NOTIFICACAO(0xf0f3, true),       // bell
    RECARREGAR(0xf01e, false);       // rotate-right

    val glyph: String get() = String(Character.toChars(code))
}

/**
 * Desenha um [AppIcon] com a mesma pegada de layout de um `Icon` do Material
 * (uma caixa quadrada de [size]); o glifo ocupa ~86% da caixa.
 */
@Composable
fun AppIconView(
    icon: AppIcon,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp,
    /** Forca o peso: `true` = Solid, `false` = Regular, `null` = o do proprio [icon]. */
    solid: Boolean? = null,
) {
    val useSolid = solid ?: !icon.regular
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = icon.glyph,
            fontFamily = if (useSolid) FaSolid else FaRegular,
            fontSize = (size.value * 0.86f).sp,
            color = tint,
            textAlign = TextAlign.Center,
        )
    }
}
