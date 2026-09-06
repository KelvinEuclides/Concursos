package mz.co.kevin.concursos.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mz.co.kevin.concursos.data.model.DetalhesFornecedorCef
import mz.co.kevin.concursos.data.model.FornecedorCef
import mz.co.kevin.concursos.ui.icons.AppIcon
import mz.co.kevin.concursos.ui.icons.AppIconView

@Composable
fun FornecedorCard(
    fornecedor: FornecedorCef,
    onCarregarDetalhes: suspend () -> Result<DetalhesFornecedorCef> = { Result.success(DetalhesFornecedorCef(fornecedor.certificado, emptyList())) }
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var mostrarDetalhes by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { mostrarDetalhes = true },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIconView(
                            AppIcon.CERTIFICADO,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            size = 13.dp
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Certificado: ${fornecedor.certificado}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(start = 8.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NUIT: ${fornecedor.nuit}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(fornecedor.nuit))
                                Toast.makeText(context, "NUIT copiado!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(22.dp)
                        ) {
                            AppIconView(
                                AppIcon.COPIAR,
                                contentDescription = "Copiar NUIT",
                                size = 12.dp,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = fornecedor.nome,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIconView(
                    AppIcon.LOCALIZACAO,
                    tint = MaterialTheme.colorScheme.primary,
                    size = 14.dp
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Província: ${fornecedor.provincia}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (fornecedor.actividades.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        AppIconView(
                            AppIcon.FORNECEDORES,
                            size = 14.dp,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = fornecedor.actividades,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Ver detalhes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(2.dp))
                AppIconView(
                    AppIcon.CHEVRON_DIREITA,
                    tint = MaterialTheme.colorScheme.primary,
                    size = 12.dp
                )
            }
        }
    }

    if (mostrarDetalhes) {
        FornecedorDetalhesSheet(
            fornecedor = fornecedor,
            onCarregarDetalhes = onCarregarDetalhes,
            onFechar = { mostrarDetalhes = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FornecedorDetalhesSheet(
    fornecedor: FornecedorCef,
    onCarregarDetalhes: suspend () -> Result<DetalhesFornecedorCef>,
    onFechar: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var carregando by remember { mutableStateOf(true) }
    var detalhes by remember { mutableStateOf<DetalhesFornecedorCef?>(null) }

    LaunchedEffect(fornecedor.certificado) {
        carregando = true
        onCarregarDetalhes().onSuccess { detalhes = it }
        carregando = false
    }

    ModalBottomSheet(onDismissRequest = onFechar, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = fornecedor.nome,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIconView(
                    AppIcon.CERTIFICADO,
                    tint = MaterialTheme.colorScheme.primary,
                    size = 14.dp
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Fornecedor certificado no CEF",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))

            LinhaDetalhe(
                icone = AppIcon.CERTIFICADO,
                rotulo = "Nº de Certificado",
                valor = fornecedor.certificado,
                onCopiar = {
                    clipboardManager.setText(AnnotatedString(fornecedor.certificado))
                    Toast.makeText(context, "Certificado copiado!", Toast.LENGTH_SHORT).show()
                }
            )
            LinhaDetalhe(
                icone = AppIcon.DOCUMENTO,
                rotulo = "NUIT",
                valor = fornecedor.nuit.ifBlank { "Não informado" },
                onCopiar = if (fornecedor.nuit.isNotBlank()) {
                    {
                        clipboardManager.setText(AnnotatedString(fornecedor.nuit))
                        Toast.makeText(context, "NUIT copiado!", Toast.LENGTH_SHORT).show()
                    }
                } else null
            )
            LinhaDetalhe(
                icone = AppIcon.LOCALIZACAO,
                rotulo = "Província",
                valor = fornecedor.provincia.ifBlank { "Não informada" }
            )
            LinhaDetalhe(
                icone = AppIcon.CALENDARIO,
                rotulo = "Data de inscrição",
                valor = fornecedor.dataInscricao.ifBlank { "Não informada" }
            )

            if (fornecedor.actividades.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Actividades cadastradas",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = fornecedor.actividades,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            val campos = detalhes?.campos.orEmpty()
            when {
                carregando -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ShimmerLine(widthFraction = 0.6f, height = 13.dp)
                    ShimmerLine(widthFraction = 0.9f, height = 18.dp)
                    ShimmerLine(widthFraction = 0.75f, height = 18.dp)
                    ShimmerLine(widthFraction = 0.5f, height = 18.dp)
                }

                campos.isNotEmpty() -> {
                    Text(
                        text = "Ramos de actividade, contactos e regime",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    campos.forEach { campo ->
                        val ehContacto = listOf("tel", "cel", "contact", "email", "e-mail", "fax")
                            .any { campo.rotulo.contains(it, ignoreCase = true) }
                        LinhaDetalhe(
                            icone = if (ehContacto) AppIcon.CONTACTO else AppIcon.FORNECEDORES,
                            rotulo = campo.rotulo,
                            valor = campo.valor,
                            onCopiar = if (ehContacto) {
                                {
                                    clipboardManager.setText(AnnotatedString(campo.valor))
                                    Toast.makeText(context, "Copiado!", Toast.LENGTH_SHORT).show()
                                }
                            } else null
                        )
                    }
                }

                else -> Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Ramos de actividade, contactos e regime não estão disponíveis " +
                            "de momento — o portal CEF pode estar em manutenção. Tenta novamente mais tarde.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LinhaDetalhe(
    icone: AppIcon,
    rotulo: String,
    valor: String,
    onCopiar: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconView(
            icone,
            tint = MaterialTheme.colorScheme.primary,
            size = 18.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rotulo,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (onCopiar != null) {
            IconButton(onClick = onCopiar) {
                AppIconView(
                    AppIcon.COPIAR,
                    contentDescription = "Copiar $rotulo",
                    size = 16.dp,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
