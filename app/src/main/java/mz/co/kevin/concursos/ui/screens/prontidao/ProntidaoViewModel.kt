package mz.co.kevin.concursos.ui.screens.prontidao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import mz.co.kevin.concursos.UfsaApplication
import mz.co.kevin.concursos.data.prontidao.ProntidaoCalculator
import mz.co.kevin.concursos.data.prontidao.ProntidaoResumo

/**
 * Feature #49: agrega as recomendações já guardadas sobre os concursos guardados.
 * Não faz chamadas de IA — só combina fluxos existentes.
 */
class ProntidaoViewModel : ViewModel() {
    private val repo = UfsaApplication.repository
    private val perfilRepo = UfsaApplication.perfilRepository

    val resumo: StateFlow<ProntidaoResumo> = combine(
        repo.observarGuardados(),
        perfilRepo.recomendacoes,
        perfilRepo.perfil,
    ) { guardados, recomendacoes, perfil ->
        ProntidaoCalculator.calcular(guardados, recomendacoes, perfil.documentosDisponiveis)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProntidaoResumo())
}
