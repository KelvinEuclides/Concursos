package mz.co.kevin.concursos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.ui.components.ConcursoCard
import mz.co.kevin.concursos.ui.components.ConcursoQaBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConcursosScreen(
    onAbrirDetalhes: (Concurso) -> Unit,
    vm: ConcursosViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val lista by vm.listaConcursos.collectAsStateWithLifecycle()
    val provincias by vm.provincias.collectAsStateWithLifecycle()
    val guardadas by vm.referenciasGuardadas.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val categorias = remember { CategoriaConcurso.entries.toTypedArray() }
    val pagerState = rememberPagerState(pageCount = { categorias.size })

    var concursoParaPerguntar by remember { mutableStateOf<Concurso?>(null) }

    LaunchedEffect(pagerState.currentPage) {
        vm.mudarCategoria(categorias[pagerState.currentPage])
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra de Pesquisa e Atualização
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.termoPesquisa,
                onValueChange = { vm.mudarPesquisa(it) },
                placeholder = { Text("Pesquisar produto, obra, serviço ou UGEA...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (state.termoPesquisa.isNotEmpty()) {
                        IconButton(onClick = { vm.mudarPesquisa("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar pesquisa")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                IconButton(onClick = { vm.atualizar() }) {
                    if (state.carregando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Atualizar concursos",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Filtros (Só TI, Províncias e Contador)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChip(
                    selected = state.apenasTI,
                    onClick = { vm.toggleTI() },
                    label = { Text("Só TI") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Computer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                )
            }

            items(provincias, key = { it }) { prov ->
                val isSelected = state.provinciaFiltro == prov
                FilterChip(
                    selected = isSelected,
                    onClick = { vm.mudarProvincia(prov) },
                    label = { Text(prov) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            if (state.apenasTI || state.provinciaFiltro.isNotEmpty() || state.termoPesquisa.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Filtros ativos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Tabs de Categoria de Concursos
        PrimaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp
        ) {
            categorias.forEachIndexed { idx, cat ->
                val selecionado = pagerState.currentPage == idx
                Tab(
                    selected = selecionado,
                    onClick = { scope.launch { pagerState.animateScrollToPage(idx) } },
                    text = {
                        Text(
                            text = cat.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal,
                            color = if (selecionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        // Pager com Lista de Concursos
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { _ ->
            when {
                state.carregando && lista.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                "A carregar concursos do portal UFSA...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                lista.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = state.erro ?: "Nenhum concurso encontrado.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (state.termoPesquisa.isNotBlank() || state.apenasTI || state.provinciaFiltro.isNotBlank()) {
                                    "Tente ajustar os filtros ou pesquisar por outro termo."
                                } else {
                                    "Não foram publicados concursos nesta categoria recentemente."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (state.termoPesquisa.isNotBlank() || state.apenasTI || state.provinciaFiltro.isNotBlank()) {
                                    OutlinedButton(onClick = {
                                        vm.mudarPesquisa("")
                                        if (state.apenasTI) vm.toggleTI()
                                        if (state.provinciaFiltro.isNotEmpty()) vm.mudarProvincia(state.provinciaFiltro)
                                    }) {
                                        Text("Limpar filtros")
                                    }
                                }
                                Button(onClick = { vm.atualizar() }) {
                                    Text("Recarregar")
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text(
                                text = "${lista.size} concurso(s) encontrado(s)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                            )
                        }

                        items(lista, key = { it.referencia }) { c ->
                            ConcursoCard(
                                concurso = c,
                                guardado = c.referencia in guardadas,
                                onClick = onAbrirDetalhes,
                                onToggleGuardar = { vm.alternarGuardado(it) },
                                onPerguntar = { concursoParaPerguntar = it }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet para fazer perguntas com IA sobre qualquer concurso tocado no card
    concursoParaPerguntar?.let { concurso ->
        ConcursoQaBottomSheet(
            concurso = concurso,
            onFechar = { concursoParaPerguntar = null }
        )
    }
}
