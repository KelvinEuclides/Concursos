package mz.co.kevin.concursos.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import mz.co.kevin.concursos.UfsaApplication
import mz.co.kevin.concursos.data.model.ConcursoGuardado
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GuardadosViewModel : ViewModel() {
    private val repo = UfsaApplication.repository

    val guardados: StateFlow<List<ConcursoGuardado>> = repo.observarGuardados()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun remover(referencia: String) {
        viewModelScope.launch { repo.removerGuardado(referencia) }
    }
}
