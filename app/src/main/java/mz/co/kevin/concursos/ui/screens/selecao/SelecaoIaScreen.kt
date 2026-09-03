package mz.co.kevin.concursos.ui.screens.selecao

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.PerfilEmpresa
import mz.co.kevin.concursos.data.model.PorteEmpresa
import mz.co.kevin.concursos.data.model.RecomendacaoConcursoIa
import mz.co.kevin.concursos.ui.util.abrirUrl

private const val TODAS_PROVINCIAS = "Todas as Províncias"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelecaoIaScreen(
    onVoltar: () -> Unit,
    onAbrirDetalhes: (Concurso) -> Unit,
    onAbrirDefinicoes: () -> Unit,
    vm: SelecaoIaViewModel = viewModel()
) {
    val modoQuestionario by vm.modoQuestionario.collectAsStateWithLifecycle()
    val perfil by vm.perfil.collectAsStateWithLifecycle()
    val geminiApiKey by vm.geminiApiKey.collectAsStateWithLifecycle()
    val recomendacoes by vm.recomendacoes.collectAsStateWithLifecycle()
    val concursosAbertos by vm.concursosAbertos.collectAsStateWithLifecycle()
    val referenciasGuardadas by vm.referenciasGuardadas.collectAsStateWithLifecycle()
    val progresso by vm.progressoAnalise.collectAsStateWithLifecycle()
    val filtroNivel by vm.filtroNivel.collectAsStateWithLifecycle()
    val mensagemErro by vm.mensagemErro.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.triagem_titulo),
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
        Box(modifier = Modifier.padding(padding)) {
            if (modoQuestionario) {
                QuestionarioWizard(
                    perfilInicial = perfil,
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
                    progresso = progresso,
                    filtroNivel = filtroNivel,
                    mensagemErro = mensagemErro,
                    onAbrirQuestionario = { vm.abrirQuestionario() },
                    onIniciarAnalise = { vm.iniciarAnaliseIa() },
                    onMudarFiltro = { vm.definirFiltroNivel(it) },
                    onAbrirDetalhes = onAbrirDetalhes,
                    onAbrirDefinicoes = onAbrirDefinicoes,
                    onAlternarGuardado = { vm.alternarGuardado(it) }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// QUESTIONÁRIO GUIADO DE PERGUNTAS (WIZARD)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionarioWizard(
    perfilInicial: PerfilEmpresa,
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

    val totalEtapas = 4
    val progresso = (etapa + 1).toFloat() / totalEtapas.toFloat()

    val titulosEtapas = listOf(
        stringResource(R.string.questionario_etapa_identificacao),
        stringResource(R.string.questionario_etapa_ramos),
        stringResource(R.string.questionario_etapa_provincias),
        stringResource(R.string.questionario_etapa_documentos)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
                        stringResource(R.string.questionario_assistente_titulo),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.questionario_etapa_de, etapa + 1, totalEtapas, titulosEtapas[etapa]),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (onCancelar != null) {
                OutlinedButton(onClick = onCancelar) {
                    Text(stringResource(R.string.acao_cancelar))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { progresso },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
        Spacer(Modifier.height(16.dp))

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
                        provincias = if (prov == TODAS_PROVINCIAS) {
                            setOf(TODAS_PROVINCIAS)
                        } else {
                            val semTodas = provincias - TODAS_PROVINCIAS
                            if (semTodas.contains(prov)) {
                                val res = semTodas - prov
                                if (res.isEmpty()) setOf(TODAS_PROVINCIAS) else res
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
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (etapa > 0) {
                OutlinedButton(onClick = { etapa-- }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.questionario_anterior))
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }

            if (etapa < totalEtapas - 1) {
                Button(onClick = { etapa++ }) {
                    Text(stringResource(R.string.questionario_proximo))
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            } else {
                Button(
                    onClick = {
                        val perfilAtualizado = PerfilEmpresa(
                            nome = nome.trim(),
                            nuit = nuit.trim(),
                            porte = porte,
                            areasAtuacao = areas.toList(),
                            especialidadesTexto = especialidadesTexto.trim(),
                            provinciasAtuacao = if (provincias.isEmpty()) listOf(TODAS_PROVINCIAS) else provincias.toList(),
                            documentosDisponiveis = documentos.toList(),
                            outrosDocumentos = outrosDocumentos.trim(),
                            configurado = true
                        )
                        onConcluir(perfilAtualizado)
                    }
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.questionario_concluir))
                }
            }
        }
    }
}

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
            stringResource(R.string.questionario_p1_titulo),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            stringResource(R.string.questionario_p1_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = nome,
            onValueChange = onNomeChange,
            label = { Text(stringResource(R.string.questionario_nome_label)) },
            placeholder = { Text(stringResource(R.string.questionario_nome_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = nuit,
            onValueChange = onNuitChange,
            label = { Text(stringResource(R.string.questionario_nuit_label)) },
            placeholder = { Text(stringResource(R.string.questionario_nuit_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text(
            stringResource(R.string.questionario_porte_label),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

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
                            stringResource(p.labelRes),
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
            stringResource(R.string.questionario_p2_titulo),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            stringResource(R.string.questionario_p2_desc),
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
            stringResource(R.string.questionario_especialidades_label),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            value = especialidadesTexto,
            onValueChange = onEspecialidadesChange,
            placeholder = { Text(stringResource(R.string.questionario_especialidades_placeholder)) },
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
            stringResource(R.string.questionario_p3_titulo),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            stringResource(R.string.questionario_p3_desc),
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
            stringResource(R.string.questionario_p4_titulo),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            stringResource(R.string.questionario_p4_desc),
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
            stringResource(R.string.questionario_outros_docs_label),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            value = outrosDocumentos,
            onValueChange = onOutrosChange,
            placeholder = { Text(stringResource(R.string.questionario_outros_docs_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
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
    progresso: ProgressoAnalise?,
    filtroNivel: String?,
    mensagemErro: String?,
    onAbrirQuestionario: () -> Unit,
    onIniciarAnalise: () -> Unit,
    onMudarFiltro: (String?) -> Unit,
    onAbrirDetalhes: (Concurso) -> Unit,
    onAbrirDefinicoes: () -> Unit,
    onAlternarGuardado: (Concurso) -> Unit
) {
    val context = LocalContext.current
    val analisando = progresso != null

    val recomendacoesOrdenadas = remember(recomendacoes) {
        recomendacoes.sortedByDescending { it.scoreCompatibilidade }
    }
    val contagemNiveis = remember(recomendacoes) {
        recomendacoes.groupingBy { it.nivelCompatibilidade.uppercase() }.eachCount()
    }
    val nAlta = contagemNiveis["ALTA"] ?: 0
    val nMedia = contagemNiveis["MEDIA"] ?: 0
    val nBaixa = contagemNiveis["BAIXA"] ?: 0

    val recomendacoesFiltradas = remember(recomendacoesOrdenadas, filtroNivel) {
        if (filtroNivel == null) recomendacoesOrdenadas
        else recomendacoesOrdenadas.filter { it.nivelCompatibilidade.equals(filtroNivel, ignoreCase = true) }
    }

    val mapaConcursos = remember(concursos) {
        concursos.associateBy { it.referencia }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                                perfil.nome.ifBlank { stringResource(R.string.triagem_empresa_padrao) },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = onAbrirQuestionario) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.triagem_empresa_editar))
                        }
                    }

                    Text(
                        stringResource(
                            R.string.triagem_empresa_resumo,
                            stringResource(perfil.porte.labelRes),
                            perfil.documentosDisponiveis.size
                        ),
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
                                    stringResource(R.string.triagem_areas_mais, perfil.areasAtuacao.size - 4),
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
                    Text(faseTexto(progresso))
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (recomendacoes.isEmpty()) stringResource(R.string.triagem_analisar)
                        else stringResource(R.string.triagem_reanalisar),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (progresso != null) {
            item {
                val frac = if (progresso.fase == FaseAnalise.ANALISANDO && progresso.total > 0) {
                    progresso.concluidos.toFloat() / progresso.total.toFloat()
                } else null
                if (frac != null) {
                    LinearProgressIndicator(
                        progress = { frac },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }

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

        if (recomendacoes.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            stringResource(R.string.triagem_resumo_titulo),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.triagem_resumo_analisados, recomendacoes.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.triagem_resumo_niveis, nAlta, nMedia, nBaixa),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        stringResource(R.string.triagem_filtrar),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    FilterChip(
                        selected = filtroNivel == null,
                        onClick = { onMudarFiltro(null) },
                        label = { Text(stringResource(R.string.triagem_filtro_todos, recomendacoes.size)) }
                    )
                    FilterChip(
                        selected = filtroNivel == "ALTA",
                        onClick = { onMudarFiltro(if (filtroNivel == "ALTA") null else "ALTA") },
                        label = { Text(stringResource(R.string.triagem_filtro_alta, nAlta)) }
                    )
                    FilterChip(
                        selected = filtroNivel == "MEDIA",
                        onClick = { onMudarFiltro(if (filtroNivel == "MEDIA") null else "MEDIA") },
                        label = { Text(stringResource(R.string.triagem_filtro_media, nMedia)) }
                    )
                    FilterChip(
                        selected = filtroNivel == "BAIXA",
                        onClick = { onMudarFiltro(if (filtroNivel == "BAIXA") null else "BAIXA") },
                        label = { Text(stringResource(R.string.triagem_filtro_baixa, nBaixa)) }
                    )
                }
            }
        }

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
                            stringResource(R.string.triagem_vazio_titulo),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.triagem_vazio_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (geminiApiKey.isBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.triagem_vazio_sem_chave),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            OutlinedButton(onClick = onAbrirDefinicoes) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.acao_abrir_definicoes))
                            }
                        }
                    }
                }
            }
        }

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

@Composable
private fun faseTexto(progresso: ProgressoAnalise?): String = when (progresso?.fase) {
    FaseAnalise.SINCRONIZANDO -> stringResource(R.string.triagem_fase_sincronizando)
    FaseAnalise.GUARDANDO -> stringResource(R.string.triagem_fase_guardando)
    FaseAnalise.ANALISANDO -> stringResource(
        R.string.triagem_fase_analisando,
        progresso.concluidos,
        progresso.total
    )
    null -> ""
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
                            stringResource(
                                R.string.triagem_card_score,
                                recomendacao.scoreCompatibilidade,
                                recomendacao.nivelCompatibilidade
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = corScore,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(onClick = onAlternarGuardado) {
                    Icon(
                        if (guardado) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = stringResource(R.string.acao_guardar),
                        tint = if (guardado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            val titulo = concurso?.objecto?.ifBlank { concurso.modalidade }
                ?: stringResource(R.string.triagem_card_concurso_fallback, recomendacao.referencia)
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(4.dp))
            Text(
                text = buildString {
                    append(stringResource(R.string.triagem_card_ref, recomendacao.referencia))
                    concurso?.ugea?.let { if (it.isNotBlank()) append(" • $it") }
                    concurso?.provincia?.let { if (it.isNotBlank()) append(" • $it") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (concurso != null && concurso.dataAbertura.isNotBlank()) {
                Text(
                    text = stringResource(R.string.triagem_card_abertura, concurso.dataAbertura),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

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
                                Text(
                                    stringResource(R.string.triagem_card_atencao, falta),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
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

            if (recomendacao.recomendacaoEstrategica.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            stringResource(R.string.triagem_card_dica_titulo),
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAbrirDetalhes,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.triagem_card_ver_detalhes))
                }

                if (concurso != null && concurso.linkDetalhes.isNotBlank()) {
                    OutlinedButton(onClick = { onAbrirSite(concurso.linkDetalhes) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
