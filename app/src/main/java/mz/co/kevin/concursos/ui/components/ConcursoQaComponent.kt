package mz.co.kevin.concursos.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mz.co.kevin.concursos.UfsaApplication
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.DetalhesConcurso
import java.util.UUID

data class MensagemQa(
    val id: String = UUID.randomUUID().toString(),
    val texto: String,
    val ehUsuario: Boolean,
    val carregando: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

val PERGUNTAS_SUGERIDAS_PADRAO = listOf(
    "Qual o produto ou serviço solicitado?",
    "Quais os requisitos e documentos obrigatórios?",
    "Qual é a data limite e onde submeter?",
    "É exigida garantia provisória?",
    "Quem pode concorrer (PME / Consórcio)?",
    "Dicas para preparar a proposta vencedora"
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ConcursoQaSection(
    concurso: Concurso,
    detalhes: DetalhesConcurso? = null,
    modifier: Modifier = Modifier,
    mostrarCardExterno: Boolean = true
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val perfilRepo = UfsaApplication.perfilRepository
    val aiService = UfsaApplication.googleAiService
    val apiKey by perfilRepo.geminiApiKey.collectAsStateWithLifecycle()
    val perfil by perfilRepo.perfil.collectAsStateWithLifecycle()

    var textoPergunta by remember { mutableStateOf("") }
    var estaRespondendo by remember { mutableStateOf(false) }
    var mostrarDialogChave by remember { mutableStateOf(false) }
    var chaveInput by remember { mutableStateOf("") }

    val mensagens = remember(concurso.referencia) {
        mutableStateListOf<MensagemQa>()
    }

    // Função para enviar pergunta
    fun submeterPergunta(pergunta: String) {
        val limpa = pergunta.trim()
        if (limpa.isBlank() || estaRespondendo) return

        mensagens.add(MensagemQa(texto = limpa, ehUsuario = true))
        val respostaPlaceholder = MensagemQa(texto = "A consultar especificações do concurso e legislação...", ehUsuario = false, carregando = true)
        mensagens.add(respostaPlaceholder)
        textoPergunta = ""
        estaRespondendo = true

        scope.launch {
            // Scroll to bottom
            listState.animateScrollToItem(mensagens.lastIndex)

            val resultado = aiService.responderPerguntaConcurso(
                apiKey = apiKey,
                perfil = perfil,
                concurso = concurso,
                detalhes = detalhes,
                pergunta = limpa
            )

            mensagens.remove(respostaPlaceholder)

            resultado.fold(
                onSuccess = { resp ->
                    mensagens.add(MensagemQa(texto = resp, ehUsuario = false, carregando = false))
                },
                onFailure = { err ->
                    mensagens.add(
                        MensagemQa(
                            texto = "Não foi possível obter a resposta: ${err.message ?: "Erro desconhecido"}",
                            ehUsuario = false,
                            carregando = false
                        )
                    )
                }
            )

            estaRespondendo = false
            listState.animateScrollToItem(mensagens.lastIndex)
        }
    }

    val conteudo = @Composable {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (mostrarCardExterno) 16.dp else 0.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Assistente do Concurso",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (apiKey.isNotBlank()) "Google AI Gemini • Decreto 79/2022" else "Modo Offline (Configure Gemini para IA completa)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (apiKey.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                if (mensagens.isNotEmpty()) {
                    IconButton(
                        onClick = { mensagens.clear() },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Limpar conversa")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Banner se a chave não estiver configurada
            if (apiKey.isBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Insira uma chave gratuita do Google AI para respostas detalhadas de IA.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            chaveInput = apiKey
                            mostrarDialogChave = true
                        }) {
                            Text("Adicionar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Perguntas sugeridas (chips)
            Text(
                text = "Perguntas frequentes sobre este concurso:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PERGUNTAS_SUGERIDAS_PADRAO.forEach { pergunta ->
                    FilterChip(
                        selected = false,
                        onClick = { submeterPergunta(pergunta) },
                        label = { Text(pergunta, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.HelpOutline,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            // Histórico de mensagens
            if (mensagens.isNotEmpty()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(mensagens, key = { it.id }) { msg ->
                        ItemMensagemQa(
                            msg = msg,
                            onCopiar = {
                                clipboardManager.setText(AnnotatedString(msg.texto))
                                Toast.makeText(context, "Resposta copiada!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // Campo para digitar qualquer pergunta
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textoPergunta,
                    onValueChange = { textoPergunta = it },
                    placeholder = { Text("Pergunte qualquer dúvida sobre o produto ou concurso...") },
                    modifier = Modifier.weight(1f),
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(Modifier.width(8.dp))

                FilledIconButton(
                    onClick = { submeterPergunta(textoPergunta) },
                    enabled = textoPergunta.isNotBlank() && !estaRespondendo,
                    modifier = Modifier.size(48.dp)
                ) {
                    if (estaRespondendo) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar pergunta"
                        )
                    }
                }
            }
        }
    }

    if (mostrarCardExterno) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            conteudo()
        }
    } else {
        Box(modifier = modifier) {
            conteudo()
        }
    }

    // Modal dialog para configurar chave rápida
    if (mostrarDialogChave) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { mostrarDialogChave = false },
            title = { Text("Chave do Google AI (Gemini)") },
            text = {
                Column {
                    Text(
                        "Para que o assistente analise os cadernos de encargos e responda a qualquer pergunta específica com inteligência artificial, configure a sua chave gratuita do Google AI Studio.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = chaveInput,
                        onValueChange = { chaveInput = it },
                        label = { Text("Chave API Key") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        perfilRepo.salvarApiKey(chaveInput)
                        mostrarDialogChave = false
                        Toast.makeText(context, "Chave salva com sucesso!", Toast.LENGTH_SHORT).show()
                    },
                    enabled = chaveInput.isNotBlank()
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogChave = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ItemMensagemQa(
    msg: MensagemQa,
    onCopiar: () -> Unit
) {
    if (msg.ehUsuario) {
        // Mensagem do Utilizador
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Text(
                    text = msg.texto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    } else {
        // Resposta da IA / Sistema
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Resposta do Consultor IA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!msg.carregando) {
                            IconButton(
                                onClick = onCopiar,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copiar",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    if (msg.carregando) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = msg.texto,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = msg.texto,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bottom Sheet que permite ao utilizador fazer perguntas sobre qualquer concurso
 * a partir de qualquer ecrã (ex: diretamente a partir de um Card de Concurso na lista).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConcursoQaBottomSheet(
    concurso: Concurso,
    onFechar: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onFechar,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = concurso.objecto.ifBlank { concurso.modalidade },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Text(
                        text = "Ref: ${concurso.referencia} • ${concurso.ugea} (${concurso.provincia})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onFechar) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ConcursoQaSection(
                concurso = concurso,
                detalhes = null,
                mostrarCardExterno = false,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
