package mz.co.kevin.concursos.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.UfsaApplication
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.ui.icons.AppIcon
import mz.co.kevin.concursos.ui.icons.AppIconView
import mz.co.kevin.concursos.ui.screens.prontidao.ProntidaoScreen
import mz.co.kevin.concursos.ui.screens.selecao.SelecaoIaScreen

private enum class Aba(@StringRes val tituloRes: Int, val icone: AppIcon) {
    CONCURSOS(R.string.nav_concursos, AppIcon.CONCURSOS),
    CEF(R.string.nav_cef_titulo, AppIcon.FORNECEDORES),
    PRONTIDAO(R.string.nav_prontidao, AppIcon.PRONTIDAO),
    DEFINICOES(R.string.nav_definicoes, AppIcon.DEFINICOES)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainerScreen() {
    var aba by rememberSaveable { mutableStateOf(Aba.CONCURSOS) }
    var concursoSelecionado by remember { mutableStateOf<Concurso?>(null) }
    var triagemAberta by rememberSaveable { mutableStateOf(false) }
    var chaveIaAberta by rememberSaveable { mutableStateOf(false) }

    val guardados by UfsaApplication.repository.observarReferenciasGuardadas()
        .collectAsStateWithLifecycle(initialValue = emptyList())

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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(aba.tituloRes),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (aba == Aba.CONCURSOS) {
                ExtendedFloatingActionButton(
                    onClick = { triagemAberta = true },
                    icon = { AppIconView(AppIcon.IA, size = 18.dp) },
                    text = { Text(stringResource(R.string.triagem_fab)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.large
                )
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp
                ) {
                    Aba.entries.forEach { item ->
                        NavigationBarItem(
                            selected = aba == item,
                            onClick = { aba = item },
                            icon = {
                                val icone = @Composable { AppIconView(item.icone, size = 22.dp) }
                                if (item == Aba.CONCURSOS && guardados.isNotEmpty()) {
                                    BadgedBox(badge = { Badge { Text("${guardados.size}") } }) { icone() }
                                } else {
                                    icone()
                                }
                            },
                            label = {
                                Text(
                                    stringResource(
                                        when (item) {
                                            Aba.CONCURSOS -> R.string.nav_concursos
                                            Aba.CEF -> R.string.nav_cef
                                            Aba.PRONTIDAO -> R.string.nav_prontidao
                                            Aba.DEFINICOES -> R.string.nav_definicoes
                                        }
                                    ),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier.padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (aba) {
                Aba.CONCURSOS -> ConcursosScreen(onAbrirDetalhes = { concursoSelecionado = it })
                Aba.CEF -> FornecedoresScreen()
                Aba.PRONTIDAO -> ProntidaoScreen()
                Aba.DEFINICOES -> DefinicoesScreen(onAbrirChaveIa = abrirChaveIa)
            }
        }
    }
}
