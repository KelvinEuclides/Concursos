package mz.co.kevin.concursos.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.UfsaApplication
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.ui.screens.selecao.SelecaoIaScreen

private enum class Aba(@StringRes val tituloRes: Int) {
    CONCURSOS(R.string.nav_concursos),
    CEF(R.string.nav_cef_titulo),
    DEFINICOES(R.string.nav_definicoes)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainerScreen() {
    var aba by rememberSaveable { mutableStateOf(Aba.CONCURSOS) }
    var concursoSelecionado by remember { mutableStateOf<Concurso?>(null) }
    var triagemAberta by rememberSaveable { mutableStateOf(false) }
    var chaveIaAberta by rememberSaveable { mutableStateOf(false) }

    val guardados by UfsaApplication.repository.observarReferenciasGuardadas().collectAsStateWithLifecycle(initialValue = emptyList())

    val abrirChaveIa = {
        concursoSelecionado = null
        triagemAberta = false
        chaveIaAberta = true
    }

    if (chaveIaAberta) {
        GoogleAiScreen(onVoltar = { chaveIaAberta = false })
        return
    }

    val selecionado = concursoSelecionado
    if (selecionado != null) {
        DetalhesConcursoScreen(
            concurso = selecionado,
            onVoltar = { concursoSelecionado = null },
            onAbrirDefinicoes = abrirChaveIa
        )
        return
    }

    if (triagemAberta) {
        SelecaoIaScreen(
            onVoltar = { triagemAberta = false },
            onAbrirDetalhes = { concursoSelecionado = it; triagemAberta = false },
            onAbrirDefinicoes = abrirChaveIa
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(aba.tituloRes),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (aba == Aba.CONCURSOS) {
                ExtendedFloatingActionButton(
                    onClick = { triagemAberta = true },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    text = { Text(stringResource(R.string.triagem_fab)) }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = aba == Aba.CONCURSOS,
                    onClick = { aba = Aba.CONCURSOS },
                    icon = {
                        if (guardados.isNotEmpty()) {
                            BadgedBox(badge = {
                                Badge {
                                    Text("${guardados.size}")
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null)
                            }
                        } else {
                            Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null)
                        }
                    },
                    label = { Text(stringResource(R.string.nav_concursos), style = MaterialTheme.typography.labelMedium) }
                )

                NavigationBarItem(
                    selected = aba == Aba.CEF,
                    onClick = { aba = Aba.CEF },
                    icon = { Icon(Icons.Default.Business, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_cef), style = MaterialTheme.typography.labelMedium) }
                )

                NavigationBarItem(
                    selected = aba == Aba.DEFINICOES,
                    onClick = { aba = Aba.DEFINICOES },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_definicoes), style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
    ) { padding ->
        Surface(modifier = Modifier.padding(padding)) {
            when (aba) {
                Aba.CONCURSOS -> ConcursosScreen(onAbrirDetalhes = { concursoSelecionado = it })
                Aba.CEF -> FornecedoresScreen()
                Aba.DEFINICOES -> DefinicoesScreen(onAbrirChaveIa = abrirChaveIa)
            }
        }
    }
}
