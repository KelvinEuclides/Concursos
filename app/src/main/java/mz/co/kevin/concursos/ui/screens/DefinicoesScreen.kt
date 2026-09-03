package mz.co.kevin.concursos.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mz.co.kevin.concursos.data.settings.AppSettings
import mz.co.kevin.concursos.data.settings.TemaApp
import mz.co.kevin.concursos.ui.util.abrirUrl

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DefinicoesScreen(vm: DefinicoesViewModel = viewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val apiKey by vm.geminiApiKey.collectAsStateWithLifecycle()
    val statusValidacao by vm.statusValidacao.collectAsStateWithLifecycle()
    val testando by vm.testando.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var inputKey by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }

    LaunchedEffect(apiKey) {
        inputKey = apiKey
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        Seccao("Aparência")

        Column(Modifier.selectableGroup()) {
            TemaApp.entries.forEach { tema ->
                ListItem(
                    headlineContent = { Text(tema.label) },
                    leadingContent = {
                        RadioButton(
                            selected = s.tema == tema,
                            onClick = { vm.definirTema(tema) }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        ListItem(
            headlineContent = { Text("Cores dinâmicas (Material You)") },
            supportingContent = { Text("Usar as cores do sistema (Android 12+)") },
            trailingContent = {
                Switch(
                    checked = s.coresDinamicas,
                    onCheckedChange = { vm.definirCoresDinamicas(it) }
                )
            }
        )

        HorizontalDivider()
        Seccao("Google AI (Gemini)")

        ListItem(
            headlineContent = { Text("Chave de API do Gemini") },
            supportingContent = {
                Text("Usada para triagem, compatibilidade e seleção de concursos para a sua empresa.")
            },
            leadingContent = {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputKey,
                onValueChange = { inputKey = it },
                label = { Text("Chave Google AI API Key") },
                placeholder = { Text("Ex: AIzaSy...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                trailingIcon = {
                    OutlinedButton(
                        onClick = { senhaVisivel = !senhaVisivel },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(if (senhaVisivel) "Ocultar" else "Ver", style = MaterialTheme.typography.labelSmall)
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { vm.salvarApiKey(inputKey) },
                    enabled = inputKey.isNotBlank() && inputKey != apiKey
                ) {
                    Text("Guardar")
                }

                OutlinedButton(
                    onClick = { vm.testarChave(inputKey) },
                    enabled = inputKey.isNotBlank() && !testando
                ) {
                    if (testando) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Testar Conexão")
                    }
                }
            }

            if (statusValidacao != null) {
                Text(
                    text = statusValidacao.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (statusValidacao.orEmpty().startsWith("✓")) {
                        MaterialTheme.colorScheme.primary
                    } else if (statusValidacao.orEmpty().startsWith("✗")) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            OutlinedButton(
                onClick = { context.abrirUrl("https://aistudio.google.com/app/apikey") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(8.dp))
                Text("Obter chave gratuita no Google AI Studio")
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Seccao("Notificações")

        ListItem(
            headlineContent = { Text("Verificação automática") },
            supportingContent = { Text("Procurar novos concursos em segundo plano e notificar") },
            trailingContent = {
                Switch(
                    checked = s.notificacoesHabilitadas,
                    onCheckedChange = { vm.definirNotificacoesHabilitadas(it) }
                )
            }
        )

        ListItem(
            headlineContent = { Text("Notificar apenas concursos de TI") },
            supportingContent = { Text("Se desligado, notifica qualquer concurso novo") },
            trailingContent = {
                Switch(
                    checked = s.notificarApenasTI,
                    enabled = s.notificacoesHabilitadas,
                    onCheckedChange = { vm.definirNotificarApenasTI(it) }
                )
            }
        )

        ListItem(
            headlineContent = { Text("Intervalo de verificação") },
            supportingContent = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppSettings.INTERVALOS_DISPONIVEIS.forEach { horas ->
                        FilterChip(
                            selected = s.intervaloHoras == horas,
                            onClick = { vm.definirIntervaloHoras(horas) },
                            enabled = s.notificacoesHabilitadas,
                            label = { Text("${horas}h") }
                        )
                    }
                }
            }
        )

        ListItem(
            headlineContent = { Text("Definições de notificação do sistema") },
            supportingContent = { Text("Abrir os canais de notificação da app") },
            trailingContent = {
                OutlinedButton(onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    runCatching { context.startActivity(intent) }
                }) { Text("Abrir") }
            }
        )

        HorizontalDivider()
        Seccao("Sobre")
        ListItem(
            headlineContent = { Text("UFSA Concursos & CEF") },
            supportingContent = { Text("Dados de www.ufsa.gov.mz — aplicação com Inteligência Artificial") }
        )
    }
}

@Composable
private fun Seccao(titulo: String) {
    Text(
        text = titulo,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}
