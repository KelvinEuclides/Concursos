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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.ui.components.FornecedorCard
import mz.co.kevin.concursos.ui.components.SkeletonLista
import mz.co.kevin.concursos.ui.icons.AppIcon
import mz.co.kevin.concursos.ui.icons.AppIconView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FornecedoresScreen(vm: FornecedoresViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val lista by vm.fornecedores.collectAsStateWithLifecycle()
    val provincias by vm.provincias.collectAsStateWithLifecycle()
    val anos by vm.anosInscricao.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var filtrosAbertos by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Barra de pesquisa (procurar pelo teclado) + filtros
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.termoPesquisa,
                onValueChange = { vm.alterarPesquisa(it) },
                placeholder = { Text(stringResource(R.string.fornecedores_pesquisar_hint), style = MaterialTheme.typography.bodyMedium, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                leadingIcon = {
                    AppIconView(
                        AppIcon.PESQUISAR,
                        tint = MaterialTheme.colorScheme.primary,
                        size = 18.dp
                    )
                },
                trailingIcon = {
                    if (state.termoPesquisa.isNotEmpty()) {
                        IconButton(onClick = { vm.alterarPesquisa("") }) {
                            AppIconView(AppIcon.LIMPAR, contentDescription = stringResource(R.string.acao_limpar_pesquisa), size = 16.dp)
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    vm.pesquisarRemoto()
                }),
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

        if (lista.isEmpty() && !state.carregando) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconView(
                        AppIcon.INFO,
                        tint = MaterialTheme.colorScheme.primary,
                        size = 15.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.fornecedores_cef_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        PullToRefreshBox(
            isRefreshing = state.carregando,
            onRefresh = { vm.pesquisarRemoto() },
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.carregando && lista.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    SkeletonLista(n = 7)
                }
            } else if (lista.isEmpty()) {
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
                        AppIcon.LOJA,
                        tint = MaterialTheme.colorScheme.outline,
                        size = 52.dp
                    )
                    Text(
                        text = state.erro ?: stringResource(R.string.fornecedores_vazio),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.fornecedores_vazio_dica),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.fornecedores_contagem, lista.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )
                    }

                    items(lista, key = { it.certificado }) { f ->
                        FornecedorCard(
                            fornecedor = f,
                            onCarregarDetalhes = { vm.carregarDetalhesFornecedor(f) }
                        )
                    }
                }
            }
        }
    }

    if (filtrosAbertos) {
        FiltrosFornecedorSheet(
            provincias = provincias,
            anos = anos,
            provinciaSelecionada = state.provinciaSelecionada,
            anoSelecionado = state.anoInscricao,
            ordem = state.ordem,
            onProvincia = vm::alterarProvincia,
            onAno = vm::alterarAno,
            onOrdem = vm::alterarOrdem,
            onLimpar = vm::limparFiltros,
            onFechar = { filtrosAbertos = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FiltrosFornecedorSheet(
    provincias: List<String>,
    anos: List<String>,
    provinciaSelecionada: String,
    anoSelecionado: String,
    ordem: OrdemFornecedor,
    onProvincia: (String) -> Unit,
    onAno: (String) -> Unit,
    onOrdem: (OrdemFornecedor) -> Unit,
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

            if (anos.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.filtro_ano_inscricao),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = anoSelecionado.isEmpty(),
                        onClick = { onAno("") },
                        label = { Text(stringResource(R.string.filtro_todos)) }
                    )
                    anos.forEach { ano ->
                        FilterChip(
                            selected = anoSelecionado == ano,
                            onClick = { onAno(ano) },
                            label = { Text(ano) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.filtro_ordenar_por),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrdemFornecedor.entries.forEach { opt ->
                    FilterChip(
                        selected = ordem == opt,
                        onClick = { onOrdem(opt) },
                        label = { Text(stringResource(opt.rotuloRes)) }
                    )
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
