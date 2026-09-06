package mz.co.kevin.concursos.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.UfsaApplication
import mz.co.kevin.concursos.data.model.ConcursoGuardado
import mz.co.kevin.concursos.ui.icons.AppIcon
import mz.co.kevin.concursos.ui.icons.AppIconView

/**
 * Feature #45 — uma pergunta em linguagem natural sobre TODOS os concursos
 * guardados. Sem histórico de conversa: pergunta → resposta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerguntarGuardadosSheet(
    guardados: List<ConcursoGuardado>,
    onFechar: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val perfilRepo = UfsaApplication.perfilRepository
    val aiService = UfsaApplication.googleAiService
    val apiKey by perfilRepo.geminiApiKey.collectAsStateWithLifecycle()
    val perfil by perfilRepo.perfil.collectAsStateWithLifecycle()

    var pergunta by remember { mutableStateOf("") }
    var aResponder by remember { mutableStateOf(false) }
    var resposta by remember { mutableStateOf<String?>(null) }

    val erroFmt = stringResource(R.string.guardados_qa_erro)
    val copiada = stringResource(R.string.qa_resposta_copiada)

    fun submeter() {
        val q = pergunta.trim()
        if (q.isBlank() || aResponder || guardados.isEmpty()) return
        aResponder = true
        resposta = null
        scope.launch {
            val r = aiService.responderPerguntaGuardados(apiKey, perfil, guardados, q)
            resposta = r.fold({ it }, { String.format(erroFmt, it.message ?: "") })
            aResponder = false
        }
    }

    ModalBottomSheet(onDismissRequest = onFechar, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = stringResource(R.string.guardados_qa_titulo),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = if (apiKey.isNotBlank() || settingsIsLocal())
                    stringResource(R.string.guardados_qa_subtitulo_online, guardados.size)
                else
                    stringResource(R.string.guardados_qa_subtitulo_offline, guardados.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.size(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = pergunta,
                    onValueChange = { pergunta = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.guardados_qa_hint),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                        )
                    },
                    enabled = !aResponder,
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submeter() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(8.dp))
                FilledIconButton(onClick = { submeter() }, enabled = !aResponder && pergunta.isNotBlank()) {
                    AppIconView(AppIcon.SETA_DIREITA, size = 18.dp, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Spacer(Modifier.size(16.dp))

            when {
                guardados.isEmpty() -> Info(stringResource(R.string.guardados_qa_vazio))
                aResponder -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.guardados_qa_a_pensar),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ShimmerLine(0.9f, 16.dp)
                    ShimmerLine(0.75f, 16.dp)
                    ShimmerLine(0.85f, 16.dp)
                    ShimmerLine(0.5f, 16.dp)
                }
                resposta != null -> Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = resposta.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(resposta.orEmpty()))
                                Toast.makeText(context, copiada, Toast.LENGTH_SHORT).show()
                            }) {
                                AppIconView(
                                    AppIcon.COPIAR,
                                    contentDescription = stringResource(R.string.guardados_qa_copiar),
                                    size = 16.dp,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.size(24.dp))
        }
    }
}

private fun settingsIsLocal(): Boolean =
    runCatching {
        UfsaApplication.settings.atual().provedorIa ==
            mz.co.kevin.concursos.data.settings.ProvedorIa.GEMMA_LOCAL
    }.getOrDefault(false)

@Composable
private fun Info(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
        )
    }
}
