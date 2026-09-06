package mz.co.kevin.concursos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.ui.icons.AppIcon
import mz.co.kevin.concursos.ui.icons.AppIconView

@Composable
fun ConcursoCard(
    concurso: Concurso,
    guardado: Boolean,
    onClick: (Concurso) -> Unit,
    onToggleGuardar: (Concurso) -> Unit,
    onPerguntar: ((Concurso) -> Unit)? = null,
    onCalendario: ((Concurso) -> Unit)? = null
) {
    var mostrarSheetPergunta by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(concurso) },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            // Linha superior: Modalidade e Acoes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = concurso.modalidade.ifBlank { "Concurso Público" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (onPerguntar != null) onPerguntar(concurso) else mostrarSheetPergunta = true
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        AppIconView(
                            AppIcon.IA,
                            contentDescription = "Perguntar à IA",
                            tint = MaterialTheme.colorScheme.primary,
                            size = 17.dp
                        )
                    }

                    if (guardado && onCalendario != null) {
                        Spacer(Modifier.width(2.dp))
                        IconButton(
                            onClick = { onCalendario(concurso) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            AppIconView(
                                AppIcon.CALENDARIO,
                                contentDescription = "Adicionar ao calendário",
                                tint = MaterialTheme.colorScheme.primary,
                                size = 17.dp
                            )
                        }
                    }

                    Spacer(Modifier.width(2.dp))

                    IconButton(
                        onClick = { onToggleGuardar(concurso) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        AppIconView(
                            AppIcon.GUARDAR,
                            contentDescription = if (guardado) "Remover dos guardados" else "Guardar concurso",
                            tint = if (guardado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 18.dp,
                            solid = guardado
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = concurso.objecto.ifBlank { concurso.modalidade },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${concurso.ugea} • ${concurso.provincia}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = concurso.referencia,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (concurso.dataAbertura.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        AppIconView(
                            AppIcon.CALENDARIO,
                            tint = MaterialTheme.colorScheme.primary,
                            size = 13.dp
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = concurso.dataAbertura,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (mostrarSheetPergunta) {
        ConcursoQaBottomSheet(
            concurso = concurso,
            onFechar = { mostrarSheetPergunta = false }
        )
    }
}
