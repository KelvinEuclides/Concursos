package mz.co.kevin.concursos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.data.settings.ProvedorIa
import mz.co.kevin.concursos.ui.components.GemmaSettingsCard
import mz.co.kevin.concursos.ui.util.abrirUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleAiScreen(
    onVoltar: () -> Unit,
    vm: DefinicoesViewModel = viewModel()
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val apiKey by vm.geminiApiKey.collectAsStateWithLifecycle()
    val statusValidacao by vm.statusValidacao.collectAsStateWithLifecycle()
    val testando by vm.testando.collectAsStateWithLifecycle()
    val gemmaStatus by vm.gemmaStatus.collectAsStateWithLifecycle()
    val testandoGemma by vm.testandoGemma.collectAsStateWithLifecycle()
    val statusTesteGemma by vm.statusTesteGemma.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var inputKey by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }

    LaunchedEffect(apiKey) { inputKey = apiKey }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.def_seccao_ia),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.acao_voltar))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.def_provedor_ia_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Column(Modifier.selectableGroup()) {
                ProvedorIa.entries.forEach { provedor ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(provedor.labelRes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        supportingContent = {
                            Text(stringResource(provedor.descRes), style = MaterialTheme.typography.bodySmall)
                        },
                        leadingContent = {
                            RadioButton(
                                selected = settings.provedorIa == provedor,
                                onClick = { vm.definirProvedorIa(provedor) }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.definirProvedorIa(provedor) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // --- Google Gemini (nuvem) ---
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.def_seccao_gemini),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.def_chave_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    label = { Text(stringResource(R.string.def_chave_label)) },
                    placeholder = { Text(stringResource(R.string.def_chave_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    trailingIcon = {
                        OutlinedButton(
                            onClick = { senhaVisivel = !senhaVisivel },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                if (senhaVisivel) stringResource(R.string.acao_ocultar) else stringResource(R.string.acao_ver),
                                style = MaterialTheme.typography.labelSmall
                            )
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
                        Text(stringResource(R.string.acao_guardar))
                    }
                    OutlinedButton(
                        onClick = { vm.testarChave(inputKey) },
                        enabled = inputKey.isNotBlank() && !testando
                    ) {
                        if (testando) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.def_testar_conexao))
                        }
                    }
                }

                if (statusValidacao != null) {
                    Text(
                        text = statusValidacao.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            statusValidacao.orEmpty().startsWith("✓") -> MaterialTheme.colorScheme.primary
                            statusValidacao.orEmpty().startsWith("✗") -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                OutlinedButton(
                    onClick = { context.abrirUrl("https://aistudio.google.com/app/apikey") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.def_obter_chave))
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.chave_ia_info), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // --- IA local (on-device) ---
            GemmaSettingsCard(
                status = gemmaStatus,
                testando = testandoGemma,
                resultadoTeste = statusTesteGemma,
                onIniciarDownload = { url -> vm.iniciarDownloadGemma(url) },
                onCancelarDownload = { vm.cancelarDownloadGemma() },
                onImportarFicheiro = { uri -> vm.importarModeloGemma(uri) },
                onEliminarModelo = { vm.eliminarModeloGemma() },
                onTestarInferencia = { vm.testarGemma() },
                onFecharResultadoTeste = { vm.fecharTesteGemma() }
            )

            Spacer(Modifier.size(16.dp))
        }
    }
}
