package mz.co.kevin.concursos.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.data.ai.GemmaModelManager
import mz.co.kevin.concursos.data.ai.GemmaStatus

@Composable
fun GemmaSettingsCard(
    status: GemmaStatus,
    testando: Boolean,
    resultadoTeste: String?,
    onIniciarDownload: () -> Unit,
    onCancelarDownload: () -> Unit,
    onImportarFicheiro: (Uri) -> Unit,
    onEliminarModelo: () -> Unit,
    onTestarInferencia: () -> Unit,
    onFecharResultadoTeste: () -> Unit,
    modifier: Modifier = Modifier
) {
    var exibirDialogExcluir by remember { mutableStateOf(false) }

    val launcherFicheiro = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportarFicheiro(uri)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cabeçalho
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.gemma_titulo),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Badge de estado
                StatusBadge(status)
            }

            Text(
                text = stringResource(R.string.gemma_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Conteúdo dinâmico conforme o estado
            when (status) {
                is GemmaStatus.NaoInstalado -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onIniciarDownload,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(stringResource(R.string.gemma_acao_descarregar), style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { launcherFicheiro.launch(arrayOf("*/*")) }
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(stringResource(R.string.gemma_acao_importar), style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.gemma_requisitos_nota),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is GemmaStatus.Descarregando -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(
                            progress = { status.progresso.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val baixadoStr = GemmaModelManager.formatarTamanho(status.baixadoBytes)
                            val totalStr = if (status.totalBytes > 0) GemmaModelManager.formatarTamanho(status.totalBytes) else "~1.3 GB"
                            val pct = status.progresso * 100f

                            Text(
                                text = stringResource(R.string.gemma_status_descarregando, pct, baixadoStr, totalStr),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedButton(
                                onClick = onCancelarDownload,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text(stringResource(R.string.gemma_acao_cancelar), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                is GemmaStatus.Carregando -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = status.mensagem,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                is GemmaStatus.Instalado -> {
                    val tamanhoFormatado = GemmaModelManager.formatarTamanho(status.tamanhoBytes)
                    Text(
                        text = stringResource(R.string.gemma_status_instalado, tamanhoFormatado),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onTestarInferencia,
                            enabled = !testando,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (testando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.size(6.dp))
                                Text(stringResource(R.string.gemma_dialog_teste_gerando), style = MaterialTheme.typography.labelSmall)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(stringResource(R.string.gemma_acao_testar), style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        OutlinedButton(
                            onClick = { exibirDialogExcluir = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(stringResource(R.string.gemma_acao_eliminar), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                is GemmaStatus.Pronto -> {
                    // Pronto também permite testar e gerir
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onTestarInferencia,
                            enabled = !testando,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (testando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(stringResource(R.string.gemma_acao_testar), style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        OutlinedButton(
                            onClick = { exibirDialogExcluir = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(stringResource(R.string.gemma_acao_eliminar), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                is GemmaStatus.Erro -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Text(
                            text = stringResource(R.string.gemma_status_erro, status.mensagem),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = onIniciarDownload, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.acao_tentar_novamente))
                        }
                        OutlinedButton(onClick = { launcherFicheiro.launch(arrayOf("*/*")) }) {
                            Text(stringResource(R.string.gemma_acao_importar))
                        }
                    }
                }
            }

            // Exibição do resultado do teste de inferência
            AnimatedVisibility(visible = resultadoTeste != null) {
                if (resultadoTeste != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.gemma_dialog_teste_titulo),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(
                                    onClick = onFecharResultadoTeste,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.acao_fechar), modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = resultadoTeste,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmação de exclusão
    if (exibirDialogExcluir) {
        AlertDialog(
            onDismissRequest = { exibirDialogExcluir = false },
            title = { Text(stringResource(R.string.gemma_dialog_eliminar_titulo)) },
            text = { Text(stringResource(R.string.gemma_dialog_eliminar_corpo)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        exibirDialogExcluir = false
                        onEliminarModelo()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.gemma_acao_eliminar))
                }
            },
            dismissButton = {
                TextButton(onClick = { exibirDialogExcluir = false }) {
                    Text(stringResource(R.string.acao_cancelar))
                }
            }
        )
    }
}

@Composable
private fun StatusBadge(status: GemmaStatus) {
    val (rotulo, corContainer, corTexto) = when (status) {
        is GemmaStatus.NaoInstalado -> Triple(
            stringResource(R.string.gemma_status_nao_instalado),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        is GemmaStatus.Descarregando -> Triple(
            "${(status.progresso * 100).toInt()}%",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        is GemmaStatus.Instalado, is GemmaStatus.Pronto -> Triple(
            stringResource(R.string.categoria_abertos),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        is GemmaStatus.Carregando -> Triple(
            "...",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        is GemmaStatus.Erro -> Triple(
            "Erro",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = corContainer
    ) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.labelSmall,
            color = corTexto,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontWeight = FontWeight.Medium
        )
    }
}
