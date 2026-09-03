package mz.co.kevin.concursos.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.ui.screens.selecao.SelecaoIaScreen

private enum class Aba(val titulo: String) {
    CONCURSOS("Concursos UFSA"),
    SELECAO_IA("Seleção IA"),
    GUARDADOS("Concursos guardados"),
    CEF("Fornecedores CEF"),
    DEFINICOES("Definições")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainerScreen() {
    var aba by rememberSaveable { mutableStateOf(Aba.CONCURSOS) }
    var concursoSelecionado by remember { mutableStateOf<Concurso?>(null) }

    val selecionado = concursoSelecionado
    if (selecionado != null) {
        DetalhesConcursoScreen(
            concurso = selecionado,
            onVoltar = { concursoSelecionado = null }
        )
        return
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(aba.titulo) }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = aba == Aba.CONCURSOS,
                    onClick = { aba = Aba.CONCURSOS },
                    icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null) },
                    label = { Text("Concursos") }
                )
                NavigationBarItem(
                    selected = aba == Aba.SELECAO_IA,
                    onClick = { aba = Aba.SELECAO_IA },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    label = { Text("Seleção IA") }
                )
                NavigationBarItem(
                    selected = aba == Aba.GUARDADOS,
                    onClick = { aba = Aba.GUARDADOS },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                    label = { Text("Guardados") }
                )
                NavigationBarItem(
                    selected = aba == Aba.CEF,
                    onClick = { aba = Aba.CEF },
                    icon = { Icon(Icons.Default.Business, contentDescription = null) },
                    label = { Text("CEF") }
                )
                NavigationBarItem(
                    selected = aba == Aba.DEFINICOES,
                    onClick = { aba = Aba.DEFINICOES },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Definições") }
                )
            }
        }
    ) { padding ->
        Surface(modifier = Modifier.padding(padding)) {
            when (aba) {
                Aba.CONCURSOS -> ConcursosScreen(onAbrirDetalhes = { concursoSelecionado = it })
                Aba.SELECAO_IA -> SelecaoIaScreen(onAbrirDetalhes = { concursoSelecionado = it })
                Aba.GUARDADOS -> GuardadosScreen()
                Aba.CEF -> FornecedoresScreen()
                Aba.DEFINICOES -> DefinicoesScreen()
            }
        }
    }
}
