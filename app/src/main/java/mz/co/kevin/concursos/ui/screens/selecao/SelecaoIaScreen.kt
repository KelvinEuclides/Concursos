package mz.co.kevin.concursos.ui.screens.selecao

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.PerfilEmpresa
import mz.co.kevin.concursos.data.model.PorteEmpresa
import mz.co.kevin.concursos.data.model.RecomendacaoConcursoIa
import mz.co.kevin.concursos.ui.util.abrirUrl

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SelecaoIaScreen(
    onAbrirDetalhes: (Concurso) -> Unit,
    vm: SelecaoIaViewModel = viewModel()
) {
    val modoQuestionario by vm.modoQuestionario.collectAsStateWithLifecycle()
    val perfil by vm.perfil.collectAsStateWithLifecycle()
    val geminiApiKey by vm.geminiApiKey.collectAsStateWithLifecycle()
    val recomendacoes by vm.recomendacoes.collectAsStateWithLifecycle()
    val concursosAbertos by vm.concursosAbertos.collectAsStateWithLifecycle()
    val referenciasGuardadas by vm.referenciasGuardadas.collectAsStateWithLifecycle()
    val analisando by vm.analisando.collectAsStateWithLifecycle()
    val filtroNivel by vm.filtroNivel.collectAsStateWithLifecycle()
    val mensagemErro by vm.mensagemErro.collectAsStateWithLifecycle()

    if (modoQuestionario) {
        QuestionarioWizard(
            perfilInicial = perfil,
            apiKeyInicial = geminiApiKey,
            onSalvarApiKey = { vm.salvarApiKey(it) },
            onTestarApiKey = { vm.testarChave(it) },
            statusChave = vm.statusChave.collectAsStateWithLifecycle().value,
            testandoChave = vm.testandoChave.collectAsStateWithLifecycle().value,
            onConcluir = { novoPerfil ->
                vm.atualizarRascunho { novoPerfil }
                vm.salvarRascunhoEFinalizar()
            },
            onCancelar = if (perfil.configurado) { { vm.fecharQuestionario() } } else null
        )
    } else {
        DashboardSelecaoIa(
            perfil = perfil,
            geminiApiKey = geminiApiKey,
            recomendacoes = recomendacoes,
            concursos = concursosAbertos,
            referenciasGuardadas = referenciasGuardadas,
            analisando = analisando,
            filtroNivel = filtroNivel,
            mensagemErro = mensagemErro,
            onAbrirQuestionario = { vm.abrirQuestionario() },
            onIniciarAnalise = { vm.iniciarAnaliseIa() },
            onMudarFiltro = { vm.definirFiltroNivel(it) },
            onAbrirDetalhes = onAbrirDetalhes,
            onAlternarGuardado = { vm.alternarGuardado(it) }
        )
    }
}

// ---------------------------------------------------------------------------
// QUESTIONÁRIO GUIADO DE PERGUNTAS (WIZARD)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionarioWizard(
    perfilInicial: PerfilEmpresa,
    apiKeyInicial: String,
    onSalvarApiKey: (String) -> Unit,
    onTestarApiKey: (String) -> Unit,
    statusChave: String?,
    testandoChave: Boolean,
    onConcluir: (PerfilEmpresa) -> Unit,
    onCancelar: (() -> Unit)?
) {
    var etapa by remember { mutableStateOf(0) }
    var nome by remember { mutableStateOf(perfilInicial.nome) }
    var nuit by remember { mutableStateOf(perfilInicial.nuit) }
    var porte by remember { mutableStateOf(perfilInicial.porte) }
    var areas by remember { mutableStateOf(perfilInicial.areasAtuacao.toSet()) }
    var especialidadesTexto by remember { mutableStateOf(perfilInicial.especialidadesTexto) }
    var provincias by remember { mutableStateOf(perfilInicial.provinciasAtuacao.toSet()) }
    var documentos by remember { mutableStateOf(perfilInicial.documentosDisponiveis.toSet()) }
    var outrosDocumentos by remember { mutableStateOf(perfilInicial.outrosDocumentos) }
    var apiKey by remember { mutableStateOf(apiKeyInicial) }
    var senhaVisivel by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val totalEtapas = 5
    val progresso = (etapa + 1).toFloat() / totalEtapas.toFloat()

    val titulosEtapas = listOf(
        "Identificação da Empresa",
        "Ramos de Atividade",
        "Províncias de Atuação",
        "Documentos & Certidões",
        "Google AI & Finalização"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Cabeçalho
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "Assistente de Perfil IA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Etapa ${etapa + 1} de $totalEtapas: ${titulosEtapas[etapa]}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (onCancelar != null) {
                OutlinedButton(onClick = onCancelar) {
                    Text("Cancelar")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { progresso },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
        )
        Spacer(Modifier.height(16.dp))

        // Conteúdo da Etapa
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            when (etapa) {
                0 -> EtapaEmpresa(
                    nome = nome,
                    onNomeChange = { nome = it },
                    nuit = nuit,
                    onNuitChange = { nuit = it },
                    porte = porte,
                    onPorteChange = { porte = it }
                )
                1 -> EtapaAreas(
                    areasSelecionadas = areas,
                    onToggleArea = { a ->
                        areas = if (areas.contains(a)) areas - a else areas + a
                    },
                    especialidadesTexto = especialidadesTexto,
                    onEspecialidadesChange = { especialidadesTexto = it }
                )
                2 -> EtapaProvincias(
                    provinciasSelecionadas = provincias,
                    onToggleProvincia = { prov ->
                        provincias = if (prov == "Todas as Províncias") {
                            setOf("Todas as Províncias")
                        } else {
                            val semTodas = provincias - "Todas as Províncias"
                            if (semTodas.contains(prov)) {
                                val res = semTodas - prov
                                if (res.isEmpty()) setOf("Todas as Províncias") else res
                            } else {
                                semTodas + prov
                            }
                        }
                    }
                )
                3 -> EtapaDocumentos(
                    documentosSelecionados = documentos,
                    onToggleDocumento = { doc ->
                        documentos = if (documentos.contains(doc)) documentos - doc else documentos + doc
                    },
                    outrosDocumentos = outrosDocumentos,
                    onOutrosChange = { outrosDocumentos = it }
                )
                4 -> EtapaChaveAi(
                    apiKey = apiKey,
                    onApiKeyChange = {
                        apiKey = it
                        onSalvarApiKey(it)
                    },
                    senhaVisivel = senhaVisivel,
                    onToggleSenhaVisivel = { senhaVisivel = !senhaVisivel },
                    onTestarApiKey = onTestarApiKey,
                    statusChave = statusChave,
                    testandoChave = testandoChave,
                    onAbrirStudio = { context.abrirUrl("https://aistudio.google.com/app/apikey") }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Botões de Navegação
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (etapa > 0) {
                OutlinedButton(onClick = { etapa-- }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Anterior")
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }

            if (etapa < totalEtapas - 1) {
                Button(onClick = { etapa++ }) {
                    Text("Próximo")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            } else {
                Button(
                    onClick = {
                        onSalvarApiKey(apiKey)
                        val perfilAtualizado = PerfilEmpresa(
                            nome = nome.trim(),
                            nuit = nuit.trim(),
                            porte = porte,
                            areasAtuacao = areas.toList(),
                            especialidadesTexto = especialidadesTexto.trim(),
                            provinciasAtuacao = if (provincias.isEmpty()) listOf("Todas as Províncias") else provincias.toList(),
                            documentosDisponiveis = documentos.toList(),
                            outrosDocumentos = outrosDocumentos.trim(),
                            configurado = true
                        )
                        onConcluir(perfilAtualizado)
                    }
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Concluir e Analisar")
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ETAPAS INDIVIDUAIS DO QUESTIONÁRIO
// ---------------------------------------------------------------------------

@Composable
private fun EtapaEmpresa(
    nome: String,
    onNomeChange: (String) -> Unit,
    nuit: String,
    onNuitChange: (String) -> Unit,
    porte: PorteEmpresa,
    onPorteChange: (PorteEmpresa) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Pergunta 1 de 5: Como se chama a sua empresa e qual o seu porte?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Estas informações ajudam a IA a contextualizar as propostas e avaliar os limites de elegibilidade da UFSA.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = nome,
            onValueChange = onNomeChange,
            label = { Text("Nome da Empresa / Razão Social") },
            placeholder = { Text("Ex: Moçambique Tech & Serviços, Lda") },
            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = nuit,
            onValueChange = onNuitChange,
            label = { Text("NUIT da Empresa (Opcional)") },
            placeholder = { Text("Ex: 400123456") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text("Porte da Empresa:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PorteEmpresa.entries.forEach { p ->
                val selecionado = porte == p
                OutlinedCard(
                    onClick = { onPorteChange(p) },
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selecionado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (selecionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            p.label,
                            fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (selecionado) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EtapaAreas(
    areasSelecionadas: Set<String>,
    onToggleArea: (String) -> Unit,
    especialidadesTexto: String,
    onEspecialidadesChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Pergunta 2 de 5: Em quais ramos de atividade a sua empresa atua?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Selecione todas as áreas em que tem capacidade de fornecer bens ou serviços ao Estado:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PerfilEmpresa.AREAS_PADRAO.forEach { area ->
                val selecionado = areasSelecionadas.contains(area)
                FilterChip(
                    selected = selecionado,
                    onClick = { onToggleArea(area) },
                    label = { Text(area) },
                    leadingIcon = if (selecionado) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Especialidades detalhadas ou palavras-chave:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            value = especialidadesTexto,
            onValueChange = onEspecialidadesChange,
            placeholder = { Text("Ex: desenvolvimento de software, cablagem de fibra óptica, manutenção de geradores, venda de consumíveis...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EtapaProvincias(
    provinciasSelecionadas: Set<String>,
    onToggleProvincia: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Pergunta 3 de 5: Em que províncias a sua empresa pode concorrer?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "A IA dará prioridade a concursos abertos nas províncias onde a sua empresa tem operações ou filial:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PerfilEmpresa.PROVINCIAS_MOCAMBIQUE.forEach { prov ->
                val selecionado = provinciasSelecionadas.contains(prov)
                FilterChip(
                    selected = selecionado,
                    onClick = { onToggleProvincia(prov) },
                    label = { Text(prov) },
                    leadingIcon = {
                        Icon(
                            if (selecionado) Icons.Default.Check else Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun EtapaDocumentos(
    documentosSelecionados: Set<String>,
    onToggleDocumento: (String) -> Unit,
    outrosDocumentos: String,
    onOutrosChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "Pergunta 4 de 5: Quais destes documentos a sua empresa possui prontos?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Concursos públicos na UFSA exigem documentos de elegibilidade jurídica, fiscal e técnica. Assinale o que a sua empresa já tem em dia:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PerfilEmpresa.DOCUMENTOS_PADRAO.forEach { doc ->
                val tem = documentosSelecionados.contains(doc)
                OutlinedCard(
                    onClick = { onToggleDocumento(doc) },
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (tem) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = tem,
                            onCheckedChange = { onToggleDocumento(doc) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            doc,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (tem) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Outros documentos, classes de alvará ou atestados:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            value = outrosDocumentos,
            onValueChange = onOutrosChange,
            placeholder = { Text("Ex: Alvará de 3ª classe em Obras Públicas, certificação Cisco, parceria autorizada...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
    }
}

@Composable
private fun EtapaChaveAi(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    senhaVisivel: Boolean,
    onToggleSenhaVisivel: () -> Unit,
    onTestarApiKey: (String) -> Unit,
    statusChave: String?,
    testandoChave: Boolean,
    onAbrirStudio: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Pergunta 5 de 5: Chave do Google AI (Gemini API)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Para fazer a triagem e análise automática dos cadernos de encargos e concursos da UFSA, informe a sua chave gratuita do Google AI Studio:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("Chave da API (Gemini)") },
            placeholder = { Text("Ex: AIzaSy...") },
            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
            trailingIcon = {
                OutlinedButton(
                    onClick = onToggleSenhaVisivel,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(if (senhaVisivel) "Ocultar" else "Ver", style = MaterialTheme.typography.labelSmall)
                }
            },
            visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onTestarApiKey(apiKey) },
                enabled = apiKey.isNotBlank() && !testandoChave
            ) {
                if (testandoChave) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Testar Chave")
                }
            }

            OutlinedButton(onClick = onAbrirStudio) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Obter Chave Grátis")
            }
        }

        if (statusChave != null) {
            Text(
                statusChave,
                style = MaterialTheme.typography.bodySmall,
                color = if (statusChave.startsWith("✓")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    "A chave de API é salva exclusivamente no seu dispositivo e usada diretamente para chamar o Gemini 2.5 Flash / 1.5 Flash.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// PAINEL DE SELEÇÃO IA (DASHBOARD)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DashboardSelecaoIa(
    perfil: PerfilEmpresa,
    geminiApiKey: String,
    recomendacoes: List<RecomendacaoConcursoIa>,
    concursos: List<Concurso>,
    referenciasGuardadas: List<String>,
    analisando: Boolean,
    filtroNivel: String?,
    mensagemErro: String?,
    onAbrirQuestionario: () -> Unit,
    onIniciarAnalise: () -> Unit,
    onMudarFiltro: (String?) -> Unit,
    onAbrirDetalhes: (Concurso) -> Unit,
    onAlternarGuardado: (Concurso) -> Unit
) {
    val context = LocalContext.current

    val recomendacoesFiltradas = remember(recomendacoes, filtroNivel) {
        if (filtroNivel == null) recomendacoes
        else recomendacoes.filter { it.nivelCompatibilidade.equals(filtroNivel, ignoreCase = true) }
    }

    val mapaConcursos = remember(concursos) {
        concursos.associateBy { it.referencia }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card Resumo da Empresa
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                perfil.nome.ifBlank { "Minha Empresa" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = onAbrirQuestionario) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar Respostas")
                        }
                    }

                    Text(
                        "${perfil.porte.label} • ${perfil.documentosDisponiveis.size} documentos em dia",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (perfil.areasAtuacao.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            perfil.areasAtuacao.take(4).forEach { area ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        area,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            if (perfil.areasAtuacao.size > 4) {
                                Text(
                                    "+${perfil.areasAtuacao.size - 4} mais",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Botão de Análise com Google AI
        item {
            Button(
                onClick = onIniciarAnalise,
                enabled = !analisando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (analisando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("O Google AI está a analisar os concursos...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (recomendacoes.isEmpty()) "Selecionar Concursos com Google AI"
                        else "Atualizar Seleção de Concursos com Google AI",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Alerta de erro se houver
        if (mensagemErro != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            mensagemErro,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Filtros de Nível de Compatibilidade
        if (recomendacoes.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filtrar:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    FilterChip(
                        selected = filtroNivel == null,
                        onClick = { onMudarFiltro(null) },
                        label = { Text("Todos (${recomendacoes.size})") }
                    )
                    FilterChip(
                        selected = filtroNivel == "ALTA",
                        onClick = { onMudarFiltro(if (filtroNivel == "ALTA") null else "ALTA") },
                        label = { Text("Alta Compatibilidade") }
                    )
                    FilterChip(
                        selected = filtroNivel == "MEDIA",
                        onClick = { onMudarFiltro(if (filtroNivel == "MEDIA") null else "MEDIA") },
                        label = { Text("Média") }
                    )
                }
            }
        }

        // Estado Vazio / Instruções
        if (recomendacoes.isEmpty() && !analisando) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Pronto para encontrar os melhores concursos!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "O Google AI irá avaliar os concursos abertos da UFSA, cruzando as exigências do edital com os seus documentos, especialidades e localização geográfica.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        if (geminiApiKey.isBlank()) {
                            Spacer(Modifier.height(4.dp))
                            OutlinedButton(onClick = onAbrirQuestionario) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Configurar Chave Google AI")
                            }
                        }
                    }
                }
            }
        }

        // Lista de Concursos Recomendados
        items(recomendacoesFiltradas, key = { it.referencia }) { rec ->
            val concurso = mapaConcursos[rec.referencia]
            CardRecomendacaoIa(
                recomendacao = rec,
                concurso = concurso,
                guardado = referenciasGuardadas.contains(rec.referencia),
                onAbrirDetalhes = { if (concurso != null) onAbrirDetalhes(concurso) },
                onAlternarGuardado = { if (concurso != null) onAlternarGuardado(concurso) },
                onAbrirSite = { url -> context.abrirUrl(url) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// CARD INDIVIDUAL DO CONCURSO SELECIONADO PELA IA
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardRecomendacaoIa(
    recomendacao: RecomendacaoConcursoIa,
    concurso: Concurso?,
    guardado: Boolean,
    onAbrirDetalhes: () -> Unit,
    onAlternarGuardado: () -> Unit,
    onAbrirSite: (String) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    val corScore = when {
        recomendacao.scoreCompatibilidade >= 75 -> Color(0xFF1B8738)
        recomendacao.scoreCompatibilidade >= 50 -> Color(0xFFD97706)
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Linha Superior com Score e Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = corScore.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, corScore)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = corScore,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${recomendacao.scoreCompatibilidade}% • Compatibilidade ${recomendacao.nivelCompatibilidade}",
                            style = MaterialTheme.typography.labelMedium,
                            color = corScore,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(onClick = onAlternarGuardado) {
                    Icon(
                        if (guardado) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Guardar",
                        tint = if (guardado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Título / Objecto
            val titulo = concurso?.objecto?.ifBlank { concurso.modalidade }
                ?: "Concurso ${recomendacao.referencia}"
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Detalhes rápidos: UGEA, Província, Referência
            Spacer(Modifier.height(4.dp))
            Text(
                text = buildString {
                    append("Ref: ${recomendacao.referencia}")
                    concurso?.ugea?.let { if (it.isNotBlank()) append(" • $it") }
                    concurso?.provincia?.let { if (it.isNotBlank()) append(" • $it") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (concurso != null && concurso.dataAbertura.isNotBlank()) {
                Text(
                    text = "Abertura / Limite: ${concurso.dataAbertura}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

            // Análise da IA em Destaque
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = recomendacao.resumoAvaliacao,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Pontos Fortes e Documentos
            if (recomendacao.pontosFortes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    recomendacao.pontosFortes.forEach { ponto ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF1B8738),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(ponto, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Documentos Exigidos vs Em Falta
            if (recomendacao.documentosEmFalta.isNotEmpty() || recomendacao.documentosExigidosProvaveis.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    recomendacao.documentosEmFalta.forEach { falta ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Atenção: $falta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    recomendacao.documentosExigidosProvaveis.take(3).forEach { doc ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(doc, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Recomendação Estratégica
            if (recomendacao.recomendacaoEstrategica.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            "Dica Estratégica da IA:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            recomendacao.recomendacaoEstrategica,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Botões de Ação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAbrirDetalhes,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ver Detalhes")
                }

                if (concurso != null && concurso.linkDetalhes.isNotBlank()) {
                    OutlinedButton(onClick = { onAbrirSite(concurso.linkDetalhes) }) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
