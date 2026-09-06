package mz.co.kevin.concursos.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Preenchimento "shimmer" — um gradiente que varre horizontalmente, usado como
 * esqueleto de carregamento em vez de spinners/barras indeterminadas.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraSmall,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-progress",
    )

    val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val highlight = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    // Faixa que atravessa a largura: -1 -> 2 mantém sempre algo visível.
    val start = (progress * 3f) - 1f
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(start * 400f, 0f),
        end = Offset((start + 1f) * 400f, 0f),
    )

    Spacer(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

@Composable
fun ShimmerLine(
    widthFraction: Float = 1f,
    height: Dp = 14.dp,
    modifier: Modifier = Modifier,
) {
    ShimmerBox(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height),
        shape = MaterialTheme.shapes.extraSmall,
    )
}

/** Esqueleto de um cartão de concurso. */
@Composable
fun SkeletonListaCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ShimmerLine(widthFraction = 0.45f, height = 18.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShimmerBox(Modifier.size(20.dp), CircleShape)
                    ShimmerBox(Modifier.size(20.dp), CircleShape)
                }
            }
            ShimmerLine(widthFraction = 0.95f, height = 16.dp)
            ShimmerLine(widthFraction = 0.7f, height = 16.dp)
            ShimmerLine(widthFraction = 0.55f, height = 12.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ShimmerLine(widthFraction = 0.35f, height = 11.dp)
                ShimmerLine(widthFraction = 0.25f, height = 11.dp)
            }
        }
    }
}

/** Coluna de [n] esqueletos de cartão, para os estados de carregamento das listas. */
@Composable
fun SkeletonLista(n: Int = 6, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(n) { SkeletonListaCard() }
    }
}
