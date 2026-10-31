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
import mz.co.kevin.concursos.data.repository.UfsaRepository
import mz.co.kevin.concursos.data.repository.PerfilEmpresaRepository

/**
 * Feature #49: agrega as recomendações já guardadas sobre os concursos guardados.
 * Não faz chamadas de IA — só combina fluxos existentes.
 */
class ProntidaoViewModel(
    private val repo: UfsaRepository,
    private val perfilRepo: PerfilEmpresaRepository,
) : ViewModel() {
    constructor() : this(UfsaApplication.repository, UfsaApplication.perfilRepository)

    val resumo: StateFlow<ProntidaoResumo> = combine(
        repo.observarGuardados(),
        perfilRepo.recomendacoes,
        perfilRepo.perfil,
    ) { guardados, recomendacoes, perfil ->
        ProntidaoCalculator.calcular(guardados, recomendacoes, perfil.documentosDisponiveis)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProntidaoResumo())
}
