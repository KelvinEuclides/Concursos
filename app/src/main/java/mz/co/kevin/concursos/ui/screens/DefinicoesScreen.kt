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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import mz.co.kevin.concursos.ui.icons.AppIcon
import mz.co.kevin.concursos.ui.icons.AppIconView
import mz.co.kevin.concursos.ui.util.abrirUrl

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DefinicoesScreen(
    onAbrirChaveIa: () -> Unit = {},
    vm: DefinicoesViewModel = viewModel()
) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val apiKey by vm.geminiApiKey.collectAsStateWithLifecycle()

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

        ListItem(
            headlineContent = { Text(stringResource(R.string.def_seccao_ia)) },
            supportingContent = {
                val motorAtivo = stringResource(s.provedorIa.labelRes)
                val estadoChave = if (s.provedorIa == ProvedorIa.GEMINI_CLOUD) {
                    if (apiKey.isNotBlank()) stringResource(R.string.def_chave_estado_ativa)
                    else stringResource(R.string.def_chave_estado_ausente)
                } else {
                    stringResource(R.string.def_provedor_ia_desc)
                }
                Column {
                    Text(motorAtivo, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = estadoChave,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            leadingContent = {
                AppIconView(AppIcon.IA, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
            },
            trailingContent = {
                AppIconView(AppIcon.CHEVRON_DIREITA, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 16.dp)
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAbrirChaveIa() }
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
                AppIconView(AppIcon.INFO, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
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
                    AppIconView(AppIcon.LEGAL, tint = MaterialTheme.colorScheme.primary, size = 18.dp)
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
                    AppIconView(AppIcon.DOCUMENTO, tint = MaterialTheme.colorScheme.primary, size = 18.dp)
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
                    AppIconView(AppIcon.AVISO, tint = MaterialTheme.colorScheme.error, size = 18.dp)
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
                AppIconView(AppIcon.SEGURANCA, size = 15.dp)
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.def_termos_legais), style = MaterialTheme.typography.labelMedium)
            }

            OutlinedButton(
                onClick = { context.abrirUrl("https://www.ufsa.gov.mz") },
                modifier = Modifier.weight(1f)
            ) {
                AppIconView(AppIcon.SETA_DIREITA, size = 15.dp)
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.detalhes_acao_portal), style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    if (exibirDialogLegal) {
        AlertDialog(
            onDismissRequest = { exibirDialogLegal = false },
            icon = { AppIconView(AppIcon.SEGURANCA, tint = MaterialTheme.colorScheme.primary) },
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
