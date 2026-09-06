package mz.co.kevin.concursos.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.data.settings.ProvedorIa
import mz.co.kevin.concursos.ui.components.GemmaSettingsCard
import mz.co.kevin.concursos.ui.icons.AppIcon
import mz.co.kevin.concursos.ui.icons.AppIconView
import mz.co.kevin.concursos.ui.util.abrirUrl

private enum class SecaoIa { HUB, GEMINI, LOCAL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleAiScreen(
    onVoltar: () -> Unit,
    vm: DefinicoesViewModel = viewModel()
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val apiKey by vm.geminiApiKey.collectAsStateWithLifecycle()

    var secao by rememberSaveable { mutableStateOf(SecaoIa.HUB) }
    val voltar = { if (secao == SecaoIa.HUB) onVoltar() else { secao = SecaoIa.HUB } }
    BackHandler(onBack = voltar)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            when (secao) {
                                SecaoIa.HUB -> R.string.def_seccao_ia
                                SecaoIa.GEMINI -> R.string.def_seccao_gemini
                                SecaoIa.LOCAL -> R.string.gemma_titulo
                            }
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = voltar) {
                        AppIconView(AppIcon.VOLTAR, contentDescription = stringResource(R.string.acao_voltar), size = 18.dp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
            when (secao) {
                SecaoIa.HUB -> HubIa(
                    provedorAtivo = settings.provedorIa,
                    temChave = apiKey.isNotBlank(),
                    onProvedor = vm::definirProvedorIa,
                    onAbrirGemini = { secao = SecaoIa.GEMINI },
                    onAbrirLocal = { secao = SecaoIa.LOCAL }
                )
                SecaoIa.GEMINI -> SeccaoGemini(vm)
                SecaoIa.LOCAL -> SeccaoLocal(vm)
            }
            Spacer(Modifier.size(16.dp))
        }
    }
}

@Composable
private fun HubIa(
    provedorAtivo: ProvedorIa,
    temChave: Boolean,
    onProvedor: (ProvedorIa) -> Unit,
    onAbrirGemini: () -> Unit,
    onAbrirLocal: () -> Unit,
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
                    Text(stringResource(provedor.labelRes), style = MaterialTheme.typography.bodyMedium)
                },
                supportingContent = {
                    Text(stringResource(provedor.descRes), style = MaterialTheme.typography.bodySmall)
                },
                leadingContent = {
                    RadioButton(
                        selected = provedorAtivo == provedor,
                        onClick = { onProvedor(provedor) }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProvedor(provedor) }
            )
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    ListItem(
        headlineContent = { Text(stringResource(R.string.def_seccao_gemini)) },
        supportingContent = {
            Text(
                if (temChave) stringResource(R.string.def_chave_estado_ativa)
                else stringResource(R.string.def_chave_estado_ausente),
                style = MaterialTheme.typography.labelSmall
            )
        },
        leadingContent = { AppIconView(AppIcon.CHAVE, tint = MaterialTheme.colorScheme.primary, size = 20.dp) },
        trailingContent = {
            AppIconView(AppIcon.CHEVRON_DIREITA, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 16.dp)
        },
        modifier = Modifier.fillMaxWidth().clickable { onAbrirGemini() }
    )

    ListItem(
        headlineContent = { Text(stringResource(R.string.gemma_titulo)) },
        supportingContent = {
            Text(stringResource(R.string.provedor_gemma_local_desc), style = MaterialTheme.typography.labelSmall)
        },
        leadingContent = { AppIconView(AppIcon.IA, tint = MaterialTheme.colorScheme.primary, size = 20.dp) },
        trailingContent = {
            AppIconView(AppIcon.CHEVRON_DIREITA, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 16.dp)
        },
        modifier = Modifier.fillMaxWidth().clickable { onAbrirLocal() }
    )
}

@Composable
private fun SeccaoGemini(vm: DefinicoesViewModel) {
    val apiKey by vm.geminiApiKey.collectAsStateWithLifecycle()
    val statusValidacao by vm.statusValidacao.collectAsStateWithLifecycle()
    val testando by vm.testando.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var inputKey by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }
    LaunchedEffect(apiKey) { inputKey = apiKey }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.def_chave_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = inputKey,
            onValueChange = { inputKey = it },
            label = { Text(stringResource(R.string.def_chave_label)) },
            placeholder = { Text(stringResource(R.string.def_chave_placeholder), maxLines = 1) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = { AppIconView(AppIcon.CHAVE, size = 16.dp) },
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
            AppIconView(AppIcon.SETA_DIREITA, size = 15.dp)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.def_obter_chave))
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIconView(AppIcon.INFO, tint = MaterialTheme.colorScheme.primary, size = 18.dp)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.chave_ia_info), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SeccaoLocal(vm: DefinicoesViewModel) {
    val gemmaStatus by vm.gemmaStatus.collectAsStateWithLifecycle()
    val testandoGemma by vm.testandoGemma.collectAsStateWithLifecycle()
    val statusTesteGemma by vm.statusTesteGemma.collectAsStateWithLifecycle()

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
}
