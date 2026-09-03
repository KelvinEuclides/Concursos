package mz.co.kevin.concursos.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mz.co.kevin.concursos.UfsaApplication
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.ui.screens.selecao.SelecaoIaScreen

private enum class Aba(val titulo: String, val subtitulo: String) {
    CONCURSOS("Concursos UFSA", "Contratação Pública Moçambique"),
    SELECAO_IA("Triagem & Assistente IA", "Compatibilidade com Google AI Gemini"),
    GUARDADOS("Concursos Guardados", "Prazos de submissão e calendário"),
    CEF("Portal CEF", "Cadastro de Empreiteiros e Fornecedores"),
    DEFINICOES("Definições", "Tema, Google AI e Notificações")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainerScreen() {
    var aba by rememberSaveable { mutableStateOf(Aba.CONCURSOS) }
    var concursoSelecionado by remember { mutableStateOf<Concurso?>(null) }

    val guardados by UfsaApplication.repository.observarReferenciasGuardadas().collectAsStateWithLifecycle(initialValue = emptyList())
    val apiKey by UfsaApplication.perfilRepository.geminiApiKey.collectAsStateWithLifecycle()

    val selecionado = concursoSelecionado
    if (selecionado != null) {
        DetalhesConcursoScreen(
            concurso = selecionado,
            onVoltar = { concursoSelecionado = null }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = aba.titulo,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = aba.subtitulo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (apiKey.isNotBlank()) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (apiKey.isNotBlank()) Icons.Default.AutoAwesome else Icons.Default.Key,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (apiKey.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (apiKey.isNotBlank()) "Gemini Ativo" else "IA Offline",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (apiKey.isNotBlank()) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = aba == Aba.CONCURSOS,
                    onClick = { aba = Aba.CONCURSOS },
                    icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null) },
                    label = { Text("Concursos", style = MaterialTheme.typography.labelMedium) }
                )

                NavigationBarItem(
                    selected = aba == Aba.SELECAO_IA,
                    onClick = { aba = Aba.SELECAO_IA },
                    icon = {
                        BadgedBox(badge = {
                            Badge {
                                Text("IA")
                            }
                        }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        }
                    },
                    label = { Text("Triagem IA", style = MaterialTheme.typography.labelMedium) }
                )

                NavigationBarItem(
                    selected = aba == Aba.GUARDADOS,
                    onClick = { aba = Aba.GUARDADOS },
                    icon = {
                        if (guardados.isNotEmpty()) {
                            BadgedBox(badge = {
                                Badge {
                                    Text("${guardados.size}")
                                }
                            }) {
                                Icon(Icons.Default.Bookmark, contentDescription = null)
                            }
                        } else {
                            Icon(Icons.Default.Bookmark, contentDescription = null)
                        }
                    },
                    label = { Text("Guardados", style = MaterialTheme.typography.labelMedium) }
                )

                NavigationBarItem(
                    selected = aba == Aba.CEF,
                    onClick = { aba = Aba.CEF },
                    icon = { Icon(Icons.Default.Business, contentDescription = null) },
                    label = { Text("CEF", style = MaterialTheme.typography.labelMedium) }
                )

                NavigationBarItem(
                    selected = aba == Aba.DEFINICOES,
                    onClick = { aba = Aba.DEFINICOES },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Definições", style = MaterialTheme.typography.labelMedium) }
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
