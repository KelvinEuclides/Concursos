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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mz.co.kevin.concursos.ui.components.FornecedorCard
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
                placeholder = { Text("Nome da empresa, NUIT ou ramo…", style = MaterialTheme.typography.bodyMedium) },
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
                            AppIconView(AppIcon.LIMPAR, contentDescription = "Limpar", size = 16.dp)
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
                    AppIconView(AppIcon.FILTRO, contentDescription = "Filtros", size = 18.dp)
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
                        text = "Cadastro Único de Empreiteiros de Obras Públicas, Fornecedores de Bens e Prestadores de Serviços (CEF).",
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "A consultar fornecedores no portal CEF…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                        text = state.erro ?: "Nenhum fornecedor encontrado.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text = "Escreva o nome da empresa, NUIT ou ramo e prima procurar no teclado. " +
                            "Puxe para baixo para recarregar toda a lista.",
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
                            text = "${lista.size} fornecedor(es)",
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

            if (anos.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Ano de inscrição",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = anoSelecionado.isEmpty(),
                        onClick = { onAno("") },
                        label = { Text("Todos") }
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
                text = "Ordenar por",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrdemFornecedor.entries.forEach { opt ->
                    FilterChip(
                        selected = ordem == opt,
                        onClick = { onOrdem(opt) },
                        label = { Text(opt.rotulo) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onFechar,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Ver resultados")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
