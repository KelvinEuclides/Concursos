package mz.co.kevin.concursos.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import mz.co.kevin.concursos.UfsaApplication
import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.model.Concurso
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConcursosUiState(
    val categoria: CategoriaConcurso = CategoriaConcurso.ABERTO,
    val apenasTI: Boolean = false,
    val provinciaFiltro: String = "",
    val termoPesquisa: String = "",
    val carregando: Boolean = false,
    val erro: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class ConcursosViewModel : ViewModel() {
    private val repo = UfsaApplication.repository
    private val _state = MutableStateFlow(ConcursosUiState())
    val state: StateFlow<ConcursosUiState> = _state.asStateFlow()

    val listaConcursos: StateFlow<List<Concurso>> = _state
        .flatMapLatest { s ->
            repo.observarConcursos(s.categoria, s.apenasTI, s.provinciaFiltro, s.termoPesquisa)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val provincias: StateFlow<List<String>> = _state
        .flatMapLatest { s -> repo.observarProvincias(s.categoria) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val referenciasGuardadas: StateFlow<Set<String>> = repo.observarReferenciasGuardadas()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        atualizar()
    }

    fun mudarCategoria(cat: CategoriaConcurso) {
        _state.update { it.copy(categoria = cat, provinciaFiltro = "") }
    }

    fun toggleTI() {
        _state.update { it.copy(apenasTI = !it.apenasTI) }
    }

    fun mudarProvincia(prov: String) {
        _state.update { it.copy(provinciaFiltro = if (it.provinciaFiltro == prov) "" else prov) }
    }

    fun mudarPesquisa(termo: String) {
        _state.update { it.copy(termoPesquisa = termo) }
    }

    fun alternarGuardado(concurso: Concurso) {
        viewModelScope.launch {
            if (concurso.referencia in referenciasGuardadas.value) {
                repo.removerGuardado(concurso.referencia)
            } else {
                repo.guardar(concurso)
            }
        }
    }

    fun atualizar() {
        viewModelScope.launch {
            _state.update { it.copy(carregando = true, erro = null) }
            try {
                // Em primeiro plano: o utilizador está a ver a lista, por isso
                // tudo o que for obtido conta como "já visto" e não gera notificação.
                repo.sincronizarConcursos(marcarComoVistos = true)
            } catch (e: Exception) {
                _state.update { it.copy(erro = e.message ?: "Erro ao atualizar concursos") }
            } finally {
                _state.update { it.copy(carregando = false) }
            }
        }
    }
}
