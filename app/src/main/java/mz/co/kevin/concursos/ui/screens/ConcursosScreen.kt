package mz.co.kevin.concursos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.ui.components.ConcursoCard
import kotlinx.coroutines.launch

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

    LaunchedEffect(pagerState.currentPage) {
        vm.mudarCategoria(categorias[pagerState.currentPage])
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.termoPesquisa,
                onValueChange = { vm.mudarPesquisa(it) },
                placeholder = { Text("Pesquisar objecto ou UGEA...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { vm.atualizar() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            modifier = Modifier.width(18.dp)
                        )
                    }
                )
            }
            items(provincias, key = { it }) { prov ->
                FilterChip(
                    selected = state.provinciaFiltro == prov,
                    onClick = { vm.mudarProvincia(prov) },
                    label = { Text(prov) }
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        PrimaryScrollableTabRow(selectedTabIndex = pagerState.currentPage) {
            categorias.forEachIndexed { idx, cat ->
                Tab(
                    selected = pagerState.currentPage == idx,
                    onClick = { scope.launch { pagerState.animateScrollToPage(idx) } },
                    text = { Text(cat.label) }
                )
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { _ ->
            when {
                state.carregando && lista.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                lista.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            state.erro ?: "Nenhum concurso nesta categoria.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(lista, key = { it.referencia }) { c ->
                            ConcursoCard(
                                concurso = c,
                                guardado = c.referencia in guardadas,
                                onClick = onAbrirDetalhes,
                                onToggleGuardar = { vm.alternarGuardado(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
