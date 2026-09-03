package mz.co.kevin.concursos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mz.co.kevin.concursos.data.model.ConcursoGuardado
import mz.co.kevin.concursos.ui.util.abrirUrl
import mz.co.kevin.concursos.ui.util.adicionarConcursoAoCalendario

@Composable
fun GuardadosScreen(vm: GuardadosViewModel = viewModel()) {
    val lista by vm.guardados.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (lista.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Ainda não guardou nenhum concurso.\nToque no marcador num concurso para o guardar aqui.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(lista, key = { it.referencia }) { g ->
            GuardadoCard(
                item = g,
                onCalendario = {
                    context.adicionarConcursoAoCalendario(
                        titulo = g.objecto.ifBlank { g.modalidade },
                        inicioSubmissao = g.dataInicioSubmissao,
                        fimSubmissao = g.dataFimSubmissao,
                        link = g.linkDetalhes,
                        requisitos = g.requisitos
                    )
                },
                onAbrirSite = { context.abrirUrl(g.linkDetalhes) },
                onRemover = { vm.remover(g.referencia) }
            )
        }
    }
}

@Composable
private fun GuardadoCard(
    item: ConcursoGuardado,
    onCalendario: () -> Unit,
    onAbrirSite: () -> Unit,
    onRemover: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.modalidade,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.objecto,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Event,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                val inicio = item.dataInicioSubmissao.ifBlank { "?" }
                val fim = item.dataFimSubmissao.ifBlank { "?" }
                Text(
                    text = "Submissão: $inicio → $fim",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(onClick = onCalendario) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Calendário")
                }
                if (item.linkDetalhes.isNotBlank()) {
                    TextButton(onClick = onAbrirSite) { Text("Site") }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onRemover) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remover")
                    Spacer(Modifier.width(4.dp))
                    Text("Remover")
                }
            }
        }
    }
}
