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
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TextButton
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
    var exibirDialogLegal by remember { mutableStateOf(false) }

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
                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
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
        Seccao("Sobre & Defesa Legal")

        ListItem(
            headlineContent = { Text("UFSA Concursos & CEF") },
            supportingContent = {
                Text("Versão 1.0 • Agregador independente com auxílio de Inteligência Artificial.")
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
                        text = "Defesa Legal e Origem dos Dados",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Os dados disponibilizados nesta aplicação (concursos, adjudicações, cancelamentos e catálogo CEF) têm origem no portal público da Unidade Funcional de Supervisão das Aquisições (UFSA - Ministério da Economia e Finanças de Moçambique: www.ufsa.gov.mz). " +
                            "Esta aplicação é um desenvolvimento independente e NÃO governamental. Não possui qualquer afiliação oficial, patrocínio, vínculo contratual ou chancela governamental com a UFSA ou com a República de Moçambique. " +
                            "Os dados têm caráter exclusivamente informativo. A consulta ao portal oficial e aos editais formais é indispensável para fins jurídicos e submissão de propostas.",
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
                        text = "Licença de Software",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "A aplicação é distribuída sob os termos da Licença Aberta MIT (Open Source). É permitido o uso livre e consulta pessoal ou profissional do aplicativo, respeitando integralmente os direitos de propriedade intelectual dos dados públicos emitidos pelo Estado de Moçambique.",
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
                        text = "Isenção Total de Garantia (Presente e Futura)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = "O SOFTWARE E TODOS OS DADOS SÃO FORNECIDOS RIGOROSAMENTE \"COMO ESTÃO\" (\"AS IS\") E \"CONFORME DISPONÍVEIS\". " +
                            "NÃO HÁ, NEM NUNCA HAVERÁ, QUALQUER TIPO DE GARANTIA, EXPRESSA OU IMPLÍCITA, incluindo garantias de exatidão, pontualidade, integridade, disponibilidade ininterrupta do serviço ou adequação a qualquer finalidade. " +
                            "O desenvolvedor não se responsabiliza por prejuízos, perda de prazos concorrenciais, propostas recusadas ou danos diretos ou indiretos decorrentes do uso desta ferramenta.",
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
                Text("Termos Legais", style = MaterialTheme.typography.labelMedium)
            }

            OutlinedButton(
                onClick = { context.abrirUrl("https://www.ufsa.gov.mz") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text("Portal UFSA", style = MaterialTheme.typography.labelMedium)
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
                    text = "Aviso Legal, Licença e Garantia",
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
                        text = "1. Defesa Legal e Origem dos Dados",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Esta aplicação é uma plataforma independente de agregação, consulta e triagem inteligente concebida exclusivamente para conveniência e produtividade informativa.\n\n" +
                                "• Os dados brutos sobre concursos públicos, adjudicações, cancelamentos e fornecedores (CEF) são extraídos de fontes acessíveis ao público no portal governamental da UFSA (www.ufsa.gov.mz).\n" +
                                "• Esta aplicação NÃO é oficial, NÃO é governamental e NÃO possui nenhuma ligação, afiliação, representação, endosso ou contrato com o Ministério da Economia e Finanças de Moçambique, com a UFSA ou qualquer outro órgão público.\n" +
                                "• Para todos os efeitos legais, contratuais, fiscais ou de submissão de propostas, os documentos oficiais emitidos pelas entidades contratantes e os publicados no portal oficial da UFSA e no Boletim da República são os únicos soberanos e juridicamente vinculativos.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    HorizontalDivider()

                    Text(
                        text = "2. Licença de Software",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "O código desta aplicação é distribuído sob a Licença MIT (Open Source):\n\n" +
                                "É concedida permissão gratuita a qualquer indivíduo para utilizar, examinar, consultar e interagir com o software, ficando condicionado à manutenção deste aviso legal e de licença em cópias relevantes.\n\n" +
                                "Todos os direitos sobre marcas governamentais, denominações públicas e conteúdos oficiais da UFSA permanecem reservados aos respetivos titulares de direito público moçambicanos.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    HorizontalDivider()

                    Text(
                        text = "3. Ausência Total de Garantia (Presente ou Futura)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "O SOFTWARE E QUAISQUER DADOS SÃO DISPONIBILIZADOS ESTRITAMENTE \"COMO ESTÃO\" (\"AS IS\") E \"CONFORME A DISPONIBILIDADE\", SEM NENHUM TIPO DE GARANTIA EXPLÍCITA OU IMPLÍCITA.\n\n" +
                                "DECLARA-SE FORMALMENTE QUE NÃO TEM E NÃO TERÁ NENHUMA GARANTIA QUANTO A:\n" +
                                "a) Exatidão, integridade, rigor, actualidade em tempo real ou validade jurídica de qualquer anúncio, concurso ou dados de fornecedor exibidos;\n" +
                                "b) Disponibilidade ininterrupta, ausência de falhas no acesso ao servidor de origem da UFSA ou em serviços de Inteligência Artificial (Google Gemini);\n" +
                                "c) Adequação a uma finalidade comercial particular, vitória em procedimentos concursais ou qualificação em concursos públicos;\n" +
                                "d) Entrega garantida ou pontual de notificações de novos concursos.\n\n" +
                                "EM CASO ALGUM O DESENVOLVEDOR OU CONTRIBUIDORES SERÃO RESPONSABILIZADOS POR QUAISQUER DANOS DIRETOS, INDIRETOS, INCIDENTAIS, ESPECIAIS OU LUCROS CESSANTES RESULTANTES DO USO OU DA IMPOSSIBILIDADE DE USO DESTA APLICAÇÃO.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { exibirDialogLegal = false }) {
                    Text("Compreendi e Fechar")
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
