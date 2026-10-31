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
import mz.co.kevin.concursos.data.ai.GoogleAiService
import mz.co.kevin.concursos.data.ai.Gemma2bService
import mz.co.kevin.concursos.data.model.ChecklistProposta
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.DetalhesConcurso
import mz.co.kevin.concursos.data.model.RecomendacaoConcursoIa
import mz.co.kevin.concursos.data.repository.PerfilEmpresaRepository
import mz.co.kevin.concursos.data.repository.UfsaRepository
import mz.co.kevin.concursos.data.settings.ProvedorIa
import mz.co.kevin.concursos.data.settings.SettingsRepository

sealed interface DetalhesEstado {
    data object Carregando : DetalhesEstado
    data class Sucesso(val detalhes: DetalhesConcurso) : DetalhesEstado
    data class Erro(val mensagem: String) : DetalhesEstado
}

class DetalhesViewModel(
    private val concurso: Concurso,
    private val repo: UfsaRepository,
    private val perfilRepo: PerfilEmpresaRepository,
    private val aiService: GoogleAiService,
    private val settings: SettingsRepository,
    private val gemma2bService: Gemma2bService,
    private val resolveString: (Int, Array<out Any?>) -> String,
) : ViewModel() {
    constructor(concurso: Concurso) : this(
        concurso,
        UfsaApplication.repository,
        UfsaApplication.perfilRepository,
        UfsaApplication.googleAiService,
        UfsaApplication.settings,
        UfsaApplication.gemma2bService,
        { resId, args -> UfsaApplication.appContext.getString(resId, *args) },
    )

    private fun s(resId: Int): String = resolveString(resId, emptyArray())
    private fun s(resId: Int, vararg args: Any?): String = resolveString(resId, args)

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

    var checklist by mutableStateOf<ChecklistProposta?>(perfilRepo.obterChecklist(concurso.referencia))
        private set

    var gerandoChecklist by mutableStateOf(false)
        private set

    var erroChecklist by mutableStateOf<String?>(null)
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
                val detalhes = repo.obterDetalhes(concurso)
                if (guardado.value) {
                    repo.atualizarRequisitosGuardado(concurso.referencia, textoRequisitos(detalhes))
                }
                DetalhesEstado.Sucesso(detalhes)
            } catch (e: Exception) {
                DetalhesEstado.Erro(e.message ?: s(R.string.detalhes_erro_carregar))
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
        val provedor = settings.atual().provedorIa
        val apiKey = perfilRepo.obterApiKeyAtual()
        if (provedor == ProvedorIa.GEMINI_CLOUD && apiKey.isBlank()) {
            erroIa = s(R.string.detalhes_ia_erro_sem_chave)
            return
        } else if (provedor == ProvedorIa.GEMMA_LOCAL && !gemma2bService.isDisponivel()) {
            erroIa = s(R.string.gemma_erro_sem_modelo)
            return
        }

        val sucesso = estado as? DetalhesEstado.Sucesso
        if (sucesso == null) {
            erroIa = s(R.string.detalhes_ia_erro_aguarde)
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
                        erroIa = s(R.string.detalhes_ia_erro_analise, err.message ?: "")
                    }
                )
            } catch (e: Exception) {
                erroIa = s(R.string.triagem_erro_inesperado, e.message ?: "")
            } finally {
                analisandoIa = false
            }
        }
    }

    fun gerarChecklist() {
        val sucesso = estado as? DetalhesEstado.Sucesso
        if (sucesso == null) {
            erroChecklist = s(R.string.detalhes_ia_erro_aguarde)
            return
        }
        if (gerandoChecklist) return

        viewModelScope.launch {
            gerandoChecklist = true
            erroChecklist = null
            try {
                val resultado = aiService.gerarChecklistProposta(
                    apiKey = perfilRepo.obterApiKeyAtual(),
                    perfil = perfilRepo.obterPerfilAtual(),
                    concurso = concurso,
                    detalhes = sucesso.detalhes,
                    analise = analiseIa,
                )
                resultado.fold(
                    onSuccess = { cl ->
                        checklist = cl
                        perfilRepo.salvarChecklist(cl)
                    },
                    onFailure = { err ->
                        erroChecklist = s(R.string.checklist_erro, err.message ?: "")
                    }
                )
            } catch (e: Exception) {
                erroChecklist = s(R.string.checklist_erro, e.message ?: "")
            } finally {
                gerandoChecklist = false
            }
        }
    }

    fun alternarItemChecklist(indice: Int) {
        val actual = checklist ?: return
        if (indice !in actual.documentos.indices) return
        val novos = actual.documentos.toMutableList().apply {
            this[indice] = this[indice].copy(concluido = !this[indice].concluido)
        }
        val actualizado = actual.copy(documentos = novos)
        checklist = actualizado
        perfilRepo.salvarChecklist(actualizado)
    }

    fun textoRequisitos(detalhes: DetalhesConcurso): String =
        detalhes.campos.joinToString("\n") { "${it.rotulo}: ${it.valor}" }
}
