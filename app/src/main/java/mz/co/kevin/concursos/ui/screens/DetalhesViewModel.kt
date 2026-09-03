package mz.co.kevin.concursos.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.UfsaApplication
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.DetalhesConcurso
import mz.co.kevin.concursos.data.model.RecomendacaoConcursoIa

sealed interface DetalhesEstado {
    data object Carregando : DetalhesEstado
    data class Sucesso(val detalhes: DetalhesConcurso) : DetalhesEstado
    data class Erro(val mensagem: String) : DetalhesEstado
}

class DetalhesViewModel(private val concurso: Concurso) : ViewModel() {
    private val repo = UfsaApplication.repository
    private val perfilRepo = UfsaApplication.perfilRepository
    private val aiService = UfsaApplication.googleAiService
    private val appContext get() = UfsaApplication.appContext

    var estado by mutableStateOf<DetalhesEstado>(DetalhesEstado.Carregando)
        private set

    val guardado: StateFlow<Boolean> = repo.observarReferenciasGuardadas()
        .map { concurso.referencia in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    var analiseIa by mutableStateOf<RecomendacaoConcursoIa?>(
        perfilRepo.recomendacoes.value.find { it.referencia == concurso.referencia }
    )
        private set

    var analisandoIa by mutableStateOf(false)
        private set

    var erroIa by mutableStateOf<String?>(null)
        private set

    init {
        // Abrir os detalhes conta como "ler" o concurso: não voltar a notificá-lo.
        viewModelScope.launch { repo.marcarConcursoVisto(concurso.referencia) }
        carregar()
    }

    fun carregar() {
        viewModelScope.launch {
            estado = DetalhesEstado.Carregando
            estado = try {
                val detalhes = repo.obterDetalhes(concurso.referencia)
                if (guardado.value) {
                    repo.atualizarRequisitosGuardado(concurso.referencia, textoRequisitos(detalhes))
                }
                DetalhesEstado.Sucesso(detalhes)
            } catch (e: Exception) {
                DetalhesEstado.Erro(e.message ?: appContext.getString(R.string.detalhes_erro_carregar))
            }
        }
    }

    fun alternarGuardado() {
        viewModelScope.launch {
            if (guardado.value) {
                repo.removerGuardado(concurso.referencia)
            } else {
                repo.guardar(concurso)
                (estado as? DetalhesEstado.Sucesso)?.let {
                    repo.atualizarRequisitosGuardado(concurso.referencia, textoRequisitos(it.detalhes))
                }
            }
        }
    }

    fun analisarComIa() {
        val apiKey = perfilRepo.obterApiKeyAtual()
        if (apiKey.isBlank()) {
            erroIa = appContext.getString(R.string.detalhes_ia_erro_sem_chave)
            return
        }

        val sucesso = estado as? DetalhesEstado.Sucesso
        if (sucesso == null) {
            erroIa = appContext.getString(R.string.detalhes_ia_erro_aguarde)
            return
        }

        viewModelScope.launch {
            analisandoIa = true
            erroIa = null
            try {
                val perfil = perfilRepo.obterPerfilAtual()
                val resultado = aiService.analisarConcursoDetalhado(
                    apiKey = apiKey,
                    perfil = perfil,
                    concurso = concurso,
                    detalhes = sucesso.detalhes
                )
                resultado.fold(
                    onSuccess = { rec ->
                        analiseIa = rec
                        perfilRepo.salvarAnaliseIndividual(rec)
                    },
                    onFailure = { err ->
                        erroIa = appContext.getString(R.string.detalhes_ia_erro_analise, err.message ?: "")
                    }
                )
            } catch (e: Exception) {
                erroIa = appContext.getString(R.string.triagem_erro_inesperado, e.message ?: "")
            } finally {
                analisandoIa = false
            }
        }
    }

    fun textoRequisitos(detalhes: DetalhesConcurso): String =
        detalhes.campos.joinToString("\n") { "${it.rotulo}: ${it.valor}" }
}
