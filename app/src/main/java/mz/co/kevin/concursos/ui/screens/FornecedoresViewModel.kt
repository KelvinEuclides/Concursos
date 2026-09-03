package mz.co.kevin.concursos.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import mz.co.kevin.concursos.UfsaApplication
import mz.co.kevin.concursos.data.model.FornecedorCef
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FornecedoresUiState(
    val provinciaSelecionada: String = "",
    val termoPesquisa: String = "",
    val carregando: Boolean = false,
    val erro: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class FornecedoresViewModel : ViewModel() {
    private val repo = UfsaApplication.repository
    private val _state = MutableStateFlow(FornecedoresUiState())
    val state: StateFlow<FornecedoresUiState> = _state.asStateFlow()

    val fornecedores: StateFlow<List<FornecedorCef>> = _state
        .flatMapLatest { s ->
            repo.observarFornecedores(s.provinciaSelecionada, s.termoPesquisa)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Províncias distintas presentes nos fornecedores já carregados (para o filtro). */
    val provincias: StateFlow<List<String>> = repo.observarProvinciasFornecedores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Carrega todos os concorrentes ao abrir (params vazios = lista completa do portal).
        pesquisarRemoto()
    }

    fun alterarPesquisa(termo: String) {
        _state.update { it.copy(termoPesquisa = termo) }
    }

    fun alterarProvincia(prov: String) {
        _state.update { it.copy(provinciaSelecionada = prov) }
    }

    fun pesquisarRemoto() {
        viewModelScope.launch {
            _state.update { it.copy(carregando = true, erro = null) }
            try {
                repo.sincronizarFornecedores(_state.value.provinciaSelecionada, _state.value.termoPesquisa)
            } catch (e: Exception) {
                _state.update { it.copy(erro = e.message ?: "Falha ao buscar fornecedores na UFSA") }
            } finally {
                _state.update { it.copy(carregando = false) }
            }
        }
    }
}
