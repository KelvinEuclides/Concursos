package mz.co.kevin.concursos.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mz.co.kevin.concursos.R
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ConcursoQaSection(
    concurso: Concurso,
    onAbrirDefinicoes: () -> Unit,
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

    val consultandoTexto = stringResource(R.string.qa_consultando)
    val erroDesconhecido = stringResource(R.string.qa_erro_desconhecido)
    val erroRespostaFmt = stringResource(R.string.qa_erro_resposta)
    val respostaCopiada = stringResource(R.string.qa_resposta_copiada)
    val perguntasSugeridas = stringArrayResource(R.array.qa_perguntas_sugeridas)

    val mensagens = remember(concurso.referencia) {
        mutableStateListOf<MensagemQa>()
    }

    fun submeterPergunta(pergunta: String) {
        val limpa = pergunta.trim()
        if (limpa.isBlank() || estaRespondendo) return

        mensagens.add(MensagemQa(texto = limpa, ehUsuario = true))
        val respostaPlaceholder = MensagemQa(texto = consultandoTexto, ehUsuario = false, carregando = true)
        mensagens.add(respostaPlaceholder)
        textoPergunta = ""
        estaRespondendo = true

        scope.launch {
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
                            texto = String.format(erroRespostaFmt, err.message ?: erroDesconhecido),
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
                            text = stringResource(R.string.qa_assistente_titulo),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (apiKey.isNotBlank()) stringResource(R.string.qa_subtitulo_online) else stringResource(R.string.qa_subtitulo_offline),
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
                        Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.qa_limpar_conversa))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

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
                            text = stringResource(R.string.qa_banner_sem_chave),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onAbrirDefinicoes) {
                            Text(stringResource(R.string.acao_abrir_definicoes), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.qa_frequentes),
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
                perguntasSugeridas.forEach { pergunta ->
                    FilterChip(
                        selected = false,
                        onClick = { submeterPergunta(pergunta) },
                        label = { Text(pergunta, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.HelpOutline,
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
                                Toast.makeText(context, respostaCopiada, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textoPergunta,
                    onValueChange = { textoPergunta = it },
                    placeholder = { Text(stringResource(R.string.qa_input_placeholder)) },
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
                            contentDescription = stringResource(R.string.qa_enviar)
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
}

@Composable
private fun ItemMensagemQa(
    msg: MensagemQa,
    onCopiar: () -> Unit
) {
    if (msg.ehUsuario) {
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
                                text = stringResource(R.string.qa_resposta_consultor),
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
                                    contentDescription = stringResource(R.string.qa_copiar),
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
    onFechar: () -> Unit,
    onAbrirDefinicoes: () -> Unit = {}
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
                        text = stringResource(
                            R.string.qa_bottomsheet_ref,
                            concurso.referencia,
                            concurso.ugea,
                            concurso.provincia
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onFechar) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.acao_fechar))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ConcursoQaSection(
                concurso = concurso,
                onAbrirDefinicoes = onAbrirDefinicoes,
                detalhes = null,
                mostrarCardExterno = false,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
