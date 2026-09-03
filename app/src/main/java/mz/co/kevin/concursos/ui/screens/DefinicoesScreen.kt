package mz.co.kevin.concursos.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.data.settings.AppSettings
import mz.co.kevin.concursos.data.settings.ProvedorIa
import mz.co.kevin.concursos.data.settings.TemaApp
import mz.co.kevin.concursos.ui.components.GemmaSettingsCard
import mz.co.kevin.concursos.ui.util.abrirUrl

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DefinicoesScreen(
    onAbrirChaveIa: () -> Unit = {},
    vm: DefinicoesViewModel = viewModel()
) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val apiKey by vm.geminiApiKey.collectAsStateWithLifecycle()
    val gemmaStatus by vm.gemmaStatus.collectAsStateWithLifecycle()
    val testandoGemma by vm.testandoGemma.collectAsStateWithLifecycle()
    val statusTesteGemma by vm.statusTesteGemma.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var exibirDialogLegal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        Seccao(stringResource(R.string.def_seccao_aparencia))

        Column(Modifier.selectableGroup()) {
            TemaApp.entries.forEach { tema ->
                ListItem(
                    headlineContent = { Text(stringResource(tema.labelRes)) },
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
            headlineContent = { Text(stringResource(R.string.def_cores_dinamicas)) },
            supportingContent = { Text(stringResource(R.string.def_cores_dinamicas_desc)) },
            trailingContent = {
                Switch(
                    checked = s.coresDinamicas,
                    onCheckedChange = { vm.definirCoresDinamicas(it) }
                )
            }
        )

        HorizontalDivider()
        Seccao(stringResource(R.string.def_seccao_idioma))

        val tagAtual = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val idiomaAtual = when {
            tagAtual.startsWith("pt") -> "pt"
            tagAtual.startsWith("en") -> "en"
            else -> ""
        }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val opcoes = listOf(
                "" to stringResource(R.string.def_idioma_sistema),
                "pt" to stringResource(R.string.def_idioma_pt),
                "en" to stringResource(R.string.def_idioma_en)
            )
            opcoes.forEach { (tag, rotulo) ->
                FilterChip(
                    selected = idiomaAtual == tag,
                    onClick = {
                        AppCompatDelegate.setApplicationLocales(
                            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
                            else LocaleListCompat.forLanguageTags(tag)
                        )
                    },
                    label = { Text(rotulo) }
                )
            }
        }

        HorizontalDivider()
        Seccao(stringResource(R.string.def_seccao_ia))

        Text(
            text = stringResource(R.string.def_provedor_ia_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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
                        Text(
                            text = stringResource(provedor.descRes),
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingContent = {
                        RadioButton(
                            selected = s.provedorIa == provedor,
                            onClick = { vm.definirProvedorIa(provedor) }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.definirProvedorIa(provedor) }
                )
            }
        }

        if (s.provedorIa == ProvedorIa.GEMINI_CLOUD) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.def_chave_titulo)) },
                supportingContent = {
                    Column {
                        Text(stringResource(R.string.def_chave_desc))
                        Text(
                            text = if (apiKey.isNotBlank()) stringResource(R.string.def_chave_estado_ativa)
                            else stringResource(R.string.def_chave_estado_ausente),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (apiKey.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                leadingContent = {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAbrirChaveIa() }
            )
        }

        Spacer(Modifier.height(4.dp))
        GemmaSettingsCard(
            status = gemmaStatus,
            testando = testandoGemma,
            resultadoTeste = statusTesteGemma,
            onIniciarDownload = { vm.iniciarDownloadGemma() },
            onCancelarDownload = { vm.cancelarDownloadGemma() },
            onImportarFicheiro = { uri -> vm.importarModeloGemma(uri) },
            onEliminarModelo = { vm.eliminarModeloGemma() },
            onTestarInferencia = { vm.testarGemma() },
            onFecharResultadoTeste = { vm.fecharTesteGemma() }
        )

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Seccao(stringResource(R.string.def_seccao_notificacoes))

        ListItem(
            headlineContent = { Text(stringResource(R.string.def_notif_auto)) },
            supportingContent = { Text(stringResource(R.string.def_notif_auto_desc)) },
            trailingContent = {
                Switch(
                    checked = s.notificacoesHabilitadas,
                    onCheckedChange = { vm.definirNotificacoesHabilitadas(it) }
                )
            }
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.def_intervalo)) },
            supportingContent = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppSettings.INTERVALOS_DISPONIVEIS.forEach { horas ->
                        FilterChip(
                            selected = s.intervaloHoras == horas,
                            onClick = { vm.definirIntervaloHoras(horas) },
                            enabled = s.notificacoesHabilitadas,
                            label = { Text(stringResource(R.string.def_intervalo_horas, horas)) }
                        )
                    }
                }
            }
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.def_notif_sistema)) },
            supportingContent = { Text(stringResource(R.string.def_notif_sistema_desc)) },
            trailingContent = {
                OutlinedButton(onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    runCatching { context.startActivity(intent) }
                }) { Text(stringResource(R.string.acao_abrir)) }
            }
        )

        HorizontalDivider()
        Seccao(stringResource(R.string.def_seccao_sobre))

        ListItem(
            headlineContent = { Text(stringResource(R.string.def_sobre_titulo)) },
            supportingContent = {
                Text(stringResource(R.string.def_sobre_versao))
            },
            leadingContent = {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Gavel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.def_legal_defesa_titulo),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = stringResource(R.string.def_legal_defesa_corpo),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.def_legal_licenca_titulo),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = stringResource(R.string.def_legal_licenca_corpo),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.def_legal_garantia_titulo),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = stringResource(R.string.def_legal_garantia_corpo),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { exibirDialogLegal = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.def_termos_legais), style = MaterialTheme.typography.labelMedium)
            }

            OutlinedButton(
                onClick = { context.abrirUrl("https://www.ufsa.gov.mz") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.detalhes_acao_portal), style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    if (exibirDialogLegal) {
        AlertDialog(
            onDismissRequest = { exibirDialogLegal = false },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text(
                    text = stringResource(R.string.def_dialog_titulo),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.def_dialog_s1_titulo),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.def_dialog_s1_corpo),
                        style = MaterialTheme.typography.bodySmall
                    )

                    HorizontalDivider()

                    Text(
                        text = stringResource(R.string.def_dialog_s2_titulo),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.def_dialog_s2_corpo),
                        style = MaterialTheme.typography.bodySmall
                    )

                    HorizontalDivider()

                    Text(
                        text = stringResource(R.string.def_dialog_s3_titulo),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = stringResource(R.string.def_dialog_s3_corpo),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { exibirDialogLegal = false }) {
                    Text(stringResource(R.string.def_dialog_fechar))
                }
            }
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
