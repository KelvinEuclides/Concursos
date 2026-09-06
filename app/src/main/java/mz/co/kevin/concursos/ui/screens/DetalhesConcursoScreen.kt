package mz.co.kevin.concursos.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.UfsaApplication
import mz.co.kevin.concursos.data.model.ChecklistProposta
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.RecomendacaoConcursoIa
import mz.co.kevin.concursos.ui.components.ConcursoQaSection
import mz.co.kevin.concursos.ui.util.abrirUrl
import mz.co.kevin.concursos.ui.util.adicionarConcursoAoCalendario

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetalhesConcursoScreen(
    concurso: Concurso,
    onVoltar: () -> Unit,
    onAbrirDefinicoes: () -> Unit = {}
) {
    val vm: DetalhesViewModel = viewModel(key = "detalhes_${concurso.referencia}") {
        DetalhesViewModel(concurso)
    }
    val guardado by vm.guardado.collectAsStateWithLifecycle()
    val apiKey by UfsaApplication.perfilRepository.geminiApiKey.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val estado = vm.estado

    var mostrarAssistente by remember { mutableStateOf(false) }

    val partilharChooser = stringResource(R.string.detalhes_partilhar_chooser)
    val partilharAssunto = stringResource(R.string.detalhes_partilhar_assunto, concurso.referencia)
    val partilharTexto = stringResource(
        R.string.detalhes_partilhar_texto,
        concurso.objecto.ifBlank { concurso.modalidade },
        concurso.referencia,
        concurso.ugea,
        concurso.provincia,
        concurso.dataAbertura,
        concurso.linkDetalhes
    )
    val refCopiada = stringResource(R.string.detalhes_ref_copiada)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.detalhes_titulo),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            concurso.referencia,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.acao_voltar))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, partilharAssunto)
                            putExtra(Intent.EXTRA_TEXT, partilharTexto)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, partilharChooser))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.detalhes_partilhar))
                    }

                    IconButton(onClick = { vm.alternarGuardado() }) {
                        Icon(
                            imageVector = if (guardado) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (guardado) stringResource(R.string.detalhes_remover) else stringResource(R.string.acao_guardar),
                            tint = if (guardado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (estado is DetalhesEstado.Sucesso) {
                ExtendedFloatingActionButton(
                    onClick = { mostrarAssistente = true },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    text = { Text(stringResource(R.string.detalhes_fab_ia)) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (estado) {
                is DetalhesEstado.Carregando -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                stringResource(R.string.detalhes_carregando),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is DetalhesEstado.Erro -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            estado.mensagem,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { vm.carregar() }) { Text(stringResource(R.string.acao_tentar_novamente)) }
                            if (concurso.linkDetalhes.isNotBlank()) {
                                FilledTonalButton(onClick = { context.abrirUrl(concurso.linkDetalhes) }) {
                                    Text(stringResource(R.string.acao_abrir_portal))
                                }
                            }
                        }
                    }
                }

                is DetalhesEstado.Sucesso -> {
                    val detalhes = estado.detalhes
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = concurso.modalidade.uppercase(),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )

                                        if (concurso.ehInformatica) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.tertiaryContainer
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.Computer,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        stringResource(R.string.detalhes_tag_ti),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        text = concurso.objecto.ifBlank { concurso.modalidade },
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Tag,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.detalhes_ref, concurso.referencia),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(concurso.referencia))
                                                Toast.makeText(context, refCopiada, Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = stringResource(R.string.detalhes_copiar_ref),
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.detalhes_entidade, concurso.ugea, concurso.provincia),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (concurso.dataAbertura.isNotBlank()) {
                                        Spacer(Modifier.height(10.dp))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Event,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = stringResource(R.string.detalhes_abertura_propostas, concurso.dataAbertura),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (concurso.dataLancamento.isNotBlank()) {
                                                        Text(
                                                            text = stringResource(R.string.detalhes_lancamento, concurso.dataLancamento),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        context.adicionarConcursoAoCalendario(
                                            titulo = concurso.objecto.ifBlank { concurso.modalidade },
                                            inicioSubmissao = concurso.dataLancamento,
                                            fimSubmissao = concurso.dataAbertura,
                                            link = concurso.linkDetalhes,
                                            requisitos = vm.textoRequisitos(detalhes)
                                        )
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.detalhes_acao_calendario))
                                }

                                if (detalhes.linkAnuncio.isNotBlank()) {
                                    ElevatedButton(onClick = { context.abrirUrl(detalhes.linkAnuncio) }) {
                                        Icon(
                                            Icons.Default.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.detalhes_acao_edital))
                                    }
                                }

                                if (detalhes.linkDocumento.isNotBlank()) {
                                    ElevatedButton(onClick = { context.abrirUrl(detalhes.linkDocumento) }) {
                                        Icon(
                                            Icons.Default.Description,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.detalhes_acao_caderno))
                                    }
                                }

                                if (concurso.linkDetalhes.isNotBlank()) {
                                    OutlinedButton(onClick = { context.abrirUrl(concurso.linkDetalhes) }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.detalhes_acao_portal))
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        stringResource(R.string.detalhes_specs_titulo),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    if (detalhes.campos.isEmpty()) {
                                        Text(
                                            stringResource(R.string.detalhes_specs_vazio),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        detalhes.campos.forEachIndexed { i, campo ->
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 10.dp)
                                            ) {
                                                Text(
                                                    campo.rotulo,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    campo.valor,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            if (i < detalhes.campos.lastIndex) {
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (mostrarAssistente) {
                        AssistenteIaSheet(
                            concurso = concurso,
                            detalhes = detalhes,
                            temChave = apiKey.isNotBlank(),
                            analisando = vm.analisandoIa,
                            analise = vm.analiseIa,
                            erro = vm.erroIa,
                            checklist = vm.checklist,
                            gerandoChecklist = vm.gerandoChecklist,
                            erroChecklist = vm.erroChecklist,
                            onAvaliar = { vm.analisarComIa() },
                            onGerarChecklist = { vm.gerarChecklist() },
                            onAlternarItemChecklist = { vm.alternarItemChecklist(it) },
                            onAbrirDefinicoes = {
                                mostrarAssistente = false
                                onAbrirDefinicoes()
                            },
                            onFechar = { mostrarAssistente = false }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AssistenteIaSheet(
    concurso: Concurso,
    detalhes: mz.co.kevin.concursos.data.model.DetalhesConcurso,
    temChave: Boolean,
    analisando: Boolean,
    analise: RecomendacaoConcursoIa?,
    erro: String?,
    checklist: ChecklistProposta?,
    gerandoChecklist: Boolean,
    erroChecklist: String?,
    onAvaliar: () -> Unit,
    onGerarChecklist: () -> Unit,
    onAlternarItemChecklist: (Int) -> Unit,
    onAbrirDefinicoes: () -> Unit,
    onFechar: () -> Unit
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.detalhes_sheet_titulo),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onFechar) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.acao_fechar))
                }
            }

            if (!temChave) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.qa_banner_sem_chave),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = onAbrirDefinicoes) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.acao_abrir_definicoes))
                        }
                    }
                }
            }

            // Triagem de compatibilidade
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.detalhes_compat_titulo),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.detalhes_compat_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FilledTonalButton(
                            onClick = onAvaliar,
                            enabled = !analisando
                        ) {
                            if (analisando) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.detalhes_compat_avaliando))
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (analise == null) stringResource(R.string.detalhes_compat_avaliar)
                                    else stringResource(R.string.detalhes_compat_reavaliar)
                                )
                            }
                        }
                    }

                    if (analisando || analise != null || erro != null) {
                        Spacer(Modifier.height(12.dp))
                        CardAvaliacaoIa(
                            analisando = analisando,
                            analise = analise,
                            erro = erro,
                            onTentarNovamente = onAvaliar
                        )
                    }
                }
            }

            ChecklistPropostaCard(
                concurso = concurso,
                checklist = checklist,
                gerando = gerandoChecklist,
                erro = erroChecklist,
                podeGerar = analise != null,
                onGerar = onGerarChecklist,
                onAlternarItem = onAlternarItemChecklist
            )

            ConcursoQaSection(
                concurso = concurso,
                onAbrirDefinicoes = onAbrirDefinicoes,
                detalhes = detalhes,
                mostrarCardExterno = false
            )
        }
    }
}

@Composable
private fun ChecklistPropostaCard(
    concurso: Concurso,
    checklist: ChecklistProposta?,
    gerando: Boolean,
    erro: String?,
    podeGerar: Boolean,
    onGerar: () -> Unit,
    onAlternarItem: (Int) -> Unit
) {
    val context = LocalContext.current
    val partilharChooser = stringResource(R.string.detalhes_partilhar_chooser)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.checklist_titulo),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.checklist_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = onGerar,
                    enabled = !gerando && (podeGerar || checklist != null)
                ) {
                    if (gerando) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.checklist_a_gerar))
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (checklist == null) stringResource(R.string.checklist_gerar)
                            else stringResource(R.string.checklist_regerar)
                        )
                    }
                }
            }

            if (!podeGerar && checklist == null && !gerando) {
                Text(
                    stringResource(R.string.checklist_requer_analise),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            erro?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            checklist?.let { cl ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Documentos
                Text(
                    stringResource(R.string.checklist_seccao_documentos),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.checklist_progresso, cl.documentosConcluidos, cl.totalDocumentos),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                cl.documentos.forEachIndexed { i, item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.concluido, onCheckedChange = { onAlternarItem(i) })
                        Spacer(Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.texto, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                stringResource(
                                    if (item.disponivel) R.string.checklist_doc_disponivel
                                    else R.string.checklist_doc_em_falta
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (item.disponivel) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                if (cl.datasChave.isNotEmpty()) {
                    Text(
                        stringResource(R.string.checklist_seccao_datas),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    cl.datasChave.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }

                if (cl.formatoEntrega.isNotBlank()) {
                    Text(
                        stringResource(R.string.checklist_seccao_formato),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(cl.formatoEntrega, style = MaterialTheme.typography.bodySmall)
                }

                if (cl.esqueleto.isNotEmpty()) {
                    Text(
                        stringResource(R.string.checklist_seccao_esqueleto),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    cl.esqueleto.forEach { sec ->
                        Text(sec.titulo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        sec.pontos.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    }
                }

                val labelDocs = stringResource(R.string.checklist_seccao_documentos)
                val labelDatas = stringResource(R.string.checklist_seccao_datas)
                val labelFormato = stringResource(R.string.checklist_seccao_formato)
                val labelEsq = stringResource(R.string.checklist_seccao_esqueleto)
                val labelFalta = stringResource(R.string.checklist_doc_em_falta)
                val tituloPartilha = stringResource(R.string.checklist_partilhar_titulo, concurso.referencia)
                OutlinedButton(
                    onClick = {
                        val texto = buildString {
                            appendLine(tituloPartilha)
                            appendLine(concurso.objecto)
                            appendLine()
                            appendLine(labelDocs)
                            cl.documentos.forEach { d ->
                                val marca = if (d.concluido) "[x]" else "[ ]"
                                val falta = if (!d.disponivel) " — $labelFalta" else ""
                                appendLine("$marca ${d.texto}$falta")
                            }
                            if (cl.datasChave.isNotEmpty()) {
                                appendLine()
                                appendLine(labelDatas)
                                cl.datasChave.forEach { appendLine("• $it") }
                            }
                            if (cl.formatoEntrega.isNotBlank()) {
                                appendLine()
                                appendLine(labelFormato)
                                appendLine(cl.formatoEntrega)
                            }
                            if (cl.esqueleto.isNotEmpty()) {
                                appendLine()
                                appendLine(labelEsq)
                                cl.esqueleto.forEach { sec ->
                                    appendLine(sec.titulo)
                                    sec.pontos.forEach { appendLine("  • $it") }
                                }
                            }
                        }
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, tituloPartilha)
                            putExtra(Intent.EXTRA_TEXT, texto)
                        }
                        context.startActivity(Intent.createChooser(send, partilharChooser))
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.checklist_partilhar))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardAvaliacaoIa(
    analisando: Boolean,
    analise: RecomendacaoConcursoIa?,
    erro: String?,
    onTentarNovamente: () -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (analisando) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        stringResource(R.string.detalhes_compat_progresso),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else if (erro != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(erro, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = onTentarNovamente) { Text(stringResource(R.string.acao_tentar), style = MaterialTheme.typography.labelSmall) }
                }
            } else if (analise != null) {
                val corScore = when {
                    analise.scoreCompatibilidade >= 75 -> Color(0xFF1B8738)
                    analise.scoreCompatibilidade >= 50 -> Color(0xFFD97706)
                    else -> MaterialTheme.colorScheme.error
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.detalhes_diag_titulo),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = corScore.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, corScore)
                    ) {
                        Text(
                            stringResource(R.string.detalhes_diag_score, analise.scoreCompatibilidade, analise.nivelCompatibilidade),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = corScore,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = analise.resumoAvaliacao,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (analise.pontosFortes.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.detalhes_pontos_fortes),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B8738)
                    )
                    analise.pontosFortes.forEach { pf ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF1B8738), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(pf, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (analise.documentosEmFalta.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.detalhes_docs_verificar),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        analise.documentosEmFalta.forEach { doc ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(doc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                if (analise.recomendacaoEstrategica.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(analise.recomendacaoEstrategica, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
