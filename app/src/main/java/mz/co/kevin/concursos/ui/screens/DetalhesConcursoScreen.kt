package mz.co.kevin.concursos.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.RecomendacaoConcursoIa
import mz.co.kevin.concursos.ui.components.ConcursoQaSection
import mz.co.kevin.concursos.ui.util.abrirUrl
import mz.co.kevin.concursos.ui.util.adicionarConcursoAoCalendario

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetalhesConcursoScreen(
    concurso: Concurso,
    onVoltar: () -> Unit
) {
    val vm: DetalhesViewModel = viewModel(key = "detalhes_${concurso.referencia}") {
        DetalhesViewModel(concurso)
    }
    val guardado by vm.guardado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val estado = vm.estado

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Detalhes do Concurso",
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Concurso: ${concurso.referencia}")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Concurso UFSA: ${concurso.objecto.ifBlank { concurso.modalidade }}\nRef: ${concurso.referencia}\nEntidade: ${concurso.ugea} (${concurso.provincia})\nAbertura: ${concurso.dataAbertura}\nLink: ${concurso.linkDetalhes}"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Partilhar Concurso"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Partilhar")
                    }

                    IconButton(onClick = { vm.alternarGuardado() }) {
                        Icon(
                            imageVector = if (guardado) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (guardado) "Remover" else "Guardar",
                            tint = if (guardado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
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
                                "A carregar especificações do concurso...",
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
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { vm.carregar() }) { Text("Tentar novamente") }
                            if (concurso.linkDetalhes.isNotBlank()) {
                                FilledTonalButton(onClick = { context.abrirUrl(concurso.linkDetalhes) }) {
                                    Text("Abrir no portal")
                                }
                            }
                        }
                    }
                }

                is DetalhesEstado.Sucesso -> {
                    val detalhes = estado.detalhes
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Card Principal com Informações do Concurso / Produto
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
                                    // Tags
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = concurso.modalidade,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

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
                                                        "TI",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                                    )
                                                }
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = concurso.provincia,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    // Objecto / Descrição do Produto
                                    Text(
                                        text = concurso.objecto.ifBlank { concurso.modalidade },
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(Modifier.height(10.dp))

                                    // Referência com botão de copiar
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
                                            text = "Ref: ${concurso.referencia}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(concurso.referencia))
                                                android.widget.Toast.makeText(context, "Referência copiada!", android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = "Copiar Referência",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }

                                    // Entidade Contratante
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
                                            text = "Entidade: ${concurso.ugea} (${concurso.provincia})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Prazos e Sessão de Abertura
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
                                                        text = "Abertura de Propostas: ${concurso.dataAbertura}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (concurso.dataLancamento.isNotBlank()) {
                                                        Text(
                                                            text = "Lançamento: ${concurso.dataLancamento}",
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

                        // Botões de Ação Rápidos (Calendário, Anúncio, Documentos, Site)
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
                                    Text("Calendário")
                                }

                                if (detalhes.linkAnuncio.isNotBlank()) {
                                    ElevatedButton(onClick = { context.abrirUrl(detalhes.linkAnuncio) }) {
                                        Icon(
                                            Icons.Default.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Edital / Anúncio")
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
                                        Text("Caderno de Encargos")
                                    }
                                }

                                if (concurso.linkDetalhes.isNotBlank()) {
                                    OutlinedButton(onClick = { context.abrirUrl(concurso.linkDetalhes) }) {
                                        Icon(
                                            Icons.Default.OpenInBrowser,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Portal UFSA")
                                    }
                                }
                            }
                        }

                        // SECÇÃO INTERATIVA DE PERGUNTAS & RESPOSTAS COM IA SOBRE O PRODUTO OU CONCURSO
                        item {
                            ConcursoQaSection(
                                concurso = concurso,
                                detalhes = detalhes,
                                mostrarCardExterno = true
                            )
                        }

                        // Card da Análise / Triagem de Compatibilidade com o Perfil da Empresa (se solicitada)
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                "Triagem de Compatibilidade",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Cruzamento com os documentos da sua empresa",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        FilledTonalButton(
                                            onClick = { vm.analisarComIa() },
                                            enabled = !vm.analisandoIa
                                        ) {
                                            if (vm.analisandoIa) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                Spacer(Modifier.width(6.dp))
                                                Text("A avaliar...")
                                            } else {
                                                Icon(
                                                    Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(if (vm.analiseIa == null) "Avaliar Perfil" else "Reavaliar")
                                            }
                                        }
                                    }

                                    if (vm.analisandoIa || vm.analiseIa != null || vm.erroIa != null) {
                                        Spacer(Modifier.height(12.dp))
                                        CardAvaliacaoIa(
                                            analisando = vm.analisandoIa,
                                            analise = vm.analiseIa,
                                            erro = vm.erroIa,
                                            onTentarNovamente = { vm.analisarComIa() }
                                        )
                                    }
                                }
                            }
                        }

                        // Especificações e Campos Extraídos do Portal / Edital
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
                                        "Especificações Técnicas & Caderno de Encargos",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    if (detalhes.campos.isEmpty()) {
                                        Text(
                                            "Nenhum campo adicional publicado no portal. Consulte o documento do concurso para mais especificações.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        detalhes.campos.forEachIndexed { i, campo ->
                                            ListItem(
                                                overlineContent = {
                                                    Text(
                                                        campo.rotulo,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                },
                                                headlineContent = {
                                                    Text(
                                                        campo.valor,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            )
                                            if (i < detalhes.campos.lastIndex) {
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
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
                        "O Google AI está a avaliar a compatibilidade deste concurso com os documentos da sua empresa...",
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
                    OutlinedButton(onClick = onTentarNovamente) { Text("Tentar", style = MaterialTheme.typography.labelSmall) }
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
                            "Diagnóstico Google AI",
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
                            "${analise.scoreCompatibilidade}% • ${analise.nivelCompatibilidade}",
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
                        "Pontos Fortes da Sua Empresa:",
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
                        "Documentos a Verificar ou Providenciar:",
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
