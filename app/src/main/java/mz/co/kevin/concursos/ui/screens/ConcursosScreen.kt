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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.UfsaApplication
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.ui.components.ConcursoCard
import mz.co.kevin.concursos.ui.icons.AppIcon
import mz.co.kevin.concursos.ui.icons.AppIconView
import mz.co.kevin.concursos.ui.components.ConcursoQaBottomSheet
import mz.co.kevin.concursos.ui.components.PerguntarGuardadosSheet
import androidx.compose.material3.FilledTonalButton
import mz.co.kevin.concursos.ui.components.SkeletonLista
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
    val guardadosDetalhe by UfsaApplication.repository.observarGuardados()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val secoes = remember { SecaoConcursos.entries.toTypedArray() }
    val pagerState = rememberPagerState(pageCount = { secoes.size })

    var concursoParaPerguntar by remember { mutableStateOf<Concurso?>(null) }
    var filtrosAbertos by remember { mutableStateOf(false) }
    var perguntaGuardadosAberta by remember { mutableStateOf(false) }

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
                placeholder = { Text(stringResource(R.string.concursos_pesquisar_hint), style = MaterialTheme.typography.bodyMedium, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                leadingIcon = {
                    AppIconView(
                        AppIcon.PESQUISAR,
                        tint = MaterialTheme.colorScheme.primary,
                        size = 18.dp
                    )
                },
                trailingIcon = {
                    if (state.termoPesquisa.isNotEmpty()) {
                        IconButton(onClick = { vm.mudarPesquisa("") }) {
                            AppIconView(AppIcon.LIMPAR, contentDescription = stringResource(R.string.acao_limpar_pesquisa), size = 16.dp)
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            BadgedBox(
                badge = { if (state.filtrosActivos > 0) Badge { Text("${state.filtrosActivos}") } }
            ) {
                FilledTonalIconButton(onClick = { filtrosAbertos = true }) {
                    AppIconView(AppIcon.FILTRO, contentDescription = stringResource(R.string.filtros_titulo), size = 18.dp)
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Secções (Abertos, Guardados, Adjudicados, Cancelados)
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            secoes.forEachIndexed { idx, secao ->
                val selecionado = pagerState.currentPage == idx
                val rotulo = when (secao) {
                    SecaoConcursos.GUARDADOS -> if (guardadas.isNotEmpty()) stringResource(R.string.secao_guardados_n, guardadas.size) else stringResource(R.string.secao_guardados)
                    else -> stringResource(secao.labelRes)
                }
                Tab(
                    selected = selecionado,
                    onClick = { scope.launch { pagerState.animateScrollToPage(idx) } },
                    text = {
                        Text(
                            text = rotulo,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
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
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            SkeletonLista(n = 7)
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
                            AppIconView(
                                AppIcon.CONCURSOS,
                                tint = MaterialTheme.colorScheme.outline,
                                size = 52.dp
                            )
                            Text(
                                text = state.erro ?: if (state.secao == SecaoConcursos.GUARDADOS) {
                                    stringResource(R.string.concursos_vazio_guardados)
                                } else {
                                    stringResource(R.string.concursos_vazio)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (state.termoPesquisa.isNotBlank() || state.filtrosActivos > 0) {
                                    stringResource(R.string.concursos_vazio_filtros)
                                } else if (state.secao == SecaoConcursos.GUARDADOS) {
                                    stringResource(R.string.concursos_vazio_guardados_dica)
                                } else {
                                    stringResource(R.string.concursos_vazio_categoria)
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
                                        Text(stringResource(R.string.acao_limpar_filtros))
                                    }
                                }
                                if (state.secao == SecaoConcursos.GUARDADOS && state.termoPesquisa.isBlank() && state.filtrosActivos == 0) {
                                    Button(onClick = { scope.launch { pagerState.animateScrollToPage(0) } }) {
                                        Text(stringResource(R.string.concursos_explorar_abertos))
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
                            if (state.secao == SecaoConcursos.GUARDADOS) {
                                item {
                                    FilledTonalButton(
                                        onClick = { perguntaGuardadosAberta = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                    ) {
                                        AppIconView(AppIcon.IA, size = 16.dp)
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.guardados_perguntar_btn))
                                    }
                                }
                            }
                            item {
                                Text(
                                    text = stringResource(R.string.concursos_contagem, lista.size),
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

    if (perguntaGuardadosAberta) {
        PerguntarGuardadosSheet(
            guardados = guardadosDetalhe,
            onFechar = { perguntaGuardadosAberta = false },
        )
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
                    text = stringResource(R.string.filtros_titulo),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onLimpar) { Text(stringResource(R.string.acao_limpar)) }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.filtro_provincia),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = provinciaSelecionada.isEmpty(),
                    onClick = { onProvincia("") },
                    label = { Text(stringResource(R.string.filtro_todas)) }
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
                text = stringResource(R.string.filtro_categoria_ia),
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
                            stringResource(R.string.filtro_classificando_ia),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    OutlinedButton(onClick = onClassificarIa) {
                        Text(stringResource(R.string.filtro_classificar_ia))
                    }
                }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = categoriaIaSelecionada.isEmpty(),
                        onClick = { onCategoriaIa("") },
                        label = { Text(stringResource(R.string.filtro_todas)) }
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
                shape = MaterialTheme.shapes.large
            ) {
                Text(stringResource(R.string.acao_ver_resultados))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
