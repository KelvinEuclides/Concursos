package mz.co.kevin.concursos.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.ui.components.ConcursoCard
import mz.co.kevin.concursos.ui.components.ConcursoQaBottomSheet
import mz.co.kevin.concursos.ui.util.adicionarConcursoAoCalendario
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConcursosScreen(
    onAbrirDetalhes: (Concurso) -> Unit,
    vm: ConcursosViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val lista by vm.listaConcursos.collectAsStateWithLifecycle()
    val provincias by vm.provincias.collectAsStateWithLifecycle()
    val categoriasIa by vm.categoriasIaDisponiveis.collectAsStateWithLifecycle()
    val categorizandoIa by vm.categorizandoComIa.collectAsStateWithLifecycle()
    val guardadas by vm.referenciasGuardadas.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val secoes = remember { SecaoConcursos.entries.toTypedArray() }
    val pagerState = rememberPagerState(pageCount = { secoes.size })

    var concursoParaPerguntar by remember { mutableStateOf<Concurso?>(null) }
    var filtrosAbertos by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        vm.mudarSecao(secoes[pagerState.currentPage])
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra de pesquisa + filtros
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.termoPesquisa,
                onValueChange = { vm.mudarPesquisa(it) },
                placeholder = { Text("Pesquisar concursos ou entidade...", style = MaterialTheme.typography.bodyMedium) },
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
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            BadgedBox(
                badge = { if (state.filtrosActivos > 0) Badge { Text("${state.filtrosActivos}") } }
            ) {
                FilledTonalIconButton(onClick = { filtrosAbertos = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filtros")
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Secções (Abertos, Guardados, Adjudicados, Cancelados)
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            secoes.forEachIndexed { idx, secao ->
                val selecionado = pagerState.currentPage == idx
                val rotulo = when (secao) {
                    SecaoConcursos.GUARDADOS -> if (guardadas.isNotEmpty()) "Guardados (${guardadas.size})" else "Guardados"
                    else -> secao.label
                }
                Tab(
                    selected = selecionado,
                    onClick = { scope.launch { pagerState.animateScrollToPage(idx) } },
                    text = {
                        Text(
                            text = rotulo,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal,
                            color = if (selecionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        // Pager com lista de concursos + pull-to-refresh
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { _ ->
            PullToRefreshBox(
                isRefreshing = state.carregando,
                onRefresh = { vm.atualizar() },
                modifier = Modifier.fillMaxSize()
            ) {
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
                        // Box com scroll para o pull-to-refresh funcionar mesmo vazio.
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Spacer(Modifier.height(48.dp))
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = state.erro ?: if (state.secao == SecaoConcursos.GUARDADOS) {
                                    "Nenhum concurso guardado."
                                } else {
                                    "Nenhum concurso encontrado."
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (state.termoPesquisa.isNotBlank() || state.filtrosActivos > 0) {
                                    "Tente ajustar os filtros ou pesquisar por outro termo."
                                } else if (state.secao == SecaoConcursos.GUARDADOS) {
                                    "Guarde concursos tocando no marcador para acompanhar prazos e receber alertas no calendário."
                                } else {
                                    "Puxe para baixo para recarregar. Não foram publicados concursos nesta categoria recentemente."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (state.termoPesquisa.isNotBlank() || state.filtrosActivos > 0) {
                                    OutlinedButton(onClick = {
                                        vm.mudarPesquisa("")
                                        vm.limparFiltros()
                                    }) {
                                        Text("Limpar filtros")
                                    }
                                }
                                if (state.secao == SecaoConcursos.GUARDADOS && state.termoPesquisa.isBlank() && state.filtrosActivos == 0) {
                                    Button(onClick = { scope.launch { pagerState.animateScrollToPage(0) } }) {
                                        Text("Explorar abertos")
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "${lista.size} concurso(s)",
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
                                    onPerguntar = { concursoParaPerguntar = it },
                                    onCalendario = {
                                        context.adicionarConcursoAoCalendario(
                                            titulo = it.objecto.ifBlank { it.modalidade },
                                            inicioSubmissao = it.dataLancamento,
                                            fimSubmissao = it.dataAbertura,
                                            link = it.linkDetalhes
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (filtrosAbertos) {
        FiltrosConcursoSheet(
            provincias = provincias,
            provinciaSelecionada = state.provinciaFiltro,
            categoriasIa = categoriasIa,
            categoriaIaSelecionada = state.categoriaIaFiltro,
            categorizandoIa = categorizandoIa,
            onProvincia = vm::mudarProvincia,
            onCategoriaIa = vm::mudarCategoriaIa,
            onClassificarIa = vm::classificarConcursosComIa,
            onLimpar = vm::limparFiltros,
            onFechar = { filtrosAbertos = false }
        )
    }

    // Modal para perguntas com IA sobre um concurso
    concursoParaPerguntar?.let { concurso ->
        ConcursoQaBottomSheet(
            concurso = concurso,
            onFechar = { concursoParaPerguntar = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FiltrosConcursoSheet(
    provincias: List<String>,
    provinciaSelecionada: String,
    categoriasIa: List<String>,
    categoriaIaSelecionada: String,
    categorizandoIa: Boolean,
    onProvincia: (String) -> Unit,
    onCategoriaIa: (String) -> Unit,
    onClassificarIa: () -> Unit,
    onLimpar: () -> Unit,
    onFechar: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onFechar, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtros",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onLimpar) { Text("Limpar") }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Província",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = provinciaSelecionada.isEmpty(),
                    onClick = { onProvincia("") },
                    label = { Text("Todas") }
                )
                provincias.forEach { prov ->
                    FilterChip(
                        selected = provinciaSelecionada == prov,
                        onClick = { onProvincia(prov) },
                        label = { Text(prov) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = "Categoria (IA)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            if (categoriasIa.isEmpty()) {
                if (categorizandoIa) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "A classificar concursos com IA…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    OutlinedButton(onClick = onClassificarIa) {
                        Text("Classificar concursos com IA")
                    }
                }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = categoriaIaSelecionada.isEmpty(),
                        onClick = { onCategoriaIa("") },
                        label = { Text("Todas") }
                    )
                    categoriasIa.forEach { cat ->
                        FilterChip(
                            selected = categoriaIaSelecionada == cat,
                            onClick = { onCategoriaIa(cat) },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onFechar,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Ver resultados", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
