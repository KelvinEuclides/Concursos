package mz.co.kevin.concursos.ui.screens.selecao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.UfsaApplication
import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.PerfilEmpresa
import mz.co.kevin.concursos.data.model.RecomendacaoConcursoIa
import mz.co.kevin.concursos.data.settings.ProvedorIa

enum class FaseAnalise { SINCRONIZANDO, ANALISANDO, GUARDANDO }

data class ProgressoAnalise(
    val fase: FaseAnalise,
    val concluidos: Int = 0,
    val total: Int = 0
)

class SelecaoIaViewModel : ViewModel() {
    private val repository = UfsaApplication.repository
    private val perfilRepository = UfsaApplication.perfilRepository
    private val aiService = UfsaApplication.googleAiService
    private val appContext get() = UfsaApplication.appContext

    val perfil: StateFlow<PerfilEmpresa> = perfilRepository.perfil
    val geminiApiKey: StateFlow<String> = perfilRepository.geminiApiKey
    val recomendacoes: StateFlow<List<RecomendacaoConcursoIa>> = perfilRepository.recomendacoes

    val concursosAbertos: StateFlow<List<Concurso>> = repository.observarConcursos(
        cat = CategoriaConcurso.ABERTO,
        provincia = "",
        busca = ""
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val referenciasGuardadas: StateFlow<List<String>> = repository.observarReferenciasGuardadas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _modoQuestionario = MutableStateFlow(false)
    val modoQuestionario: StateFlow<Boolean> = _modoQuestionario.asStateFlow()

    private val _etapaQuestionario = MutableStateFlow(0)
    val etapaQuestionario: StateFlow<Int> = _etapaQuestionario.asStateFlow()

    private val _perfilRascunho = MutableStateFlow(perfilRepository.obterPerfilAtual())
    val perfilRascunho: StateFlow<PerfilEmpresa> = _perfilRascunho.asStateFlow()

    private val _progressoAnalise = MutableStateFlow<ProgressoAnalise?>(null)
    val progressoAnalise: StateFlow<ProgressoAnalise?> = _progressoAnalise.asStateFlow()

    val analisando: Boolean get() = _progressoAnalise.value != null

    private val _filtroNivel = MutableStateFlow<String?>(null)
    val filtroNivel: StateFlow<String?> = _filtroNivel.asStateFlow()

    private val _mensagemErro = MutableStateFlow<String?>(null)
    val mensagemErro: StateFlow<String?> = _mensagemErro.asStateFlow()

    init {
        // Se o perfil ainda não foi configurado, abre o questionário para fazer as perguntas primeiro
        if (!perfilRepository.obterPerfilAtual().configurado) {
            _modoQuestionario.value = true
        }
    }

    fun abrirQuestionario() {
        _perfilRascunho.value = perfil.value
        _etapaQuestionario.value = 0
        _modoQuestionario.value = true
        _mensagemErro.value = null
    }

    fun fecharQuestionario() {
        _modoQuestionario.value = false
    }

    fun definirEtapa(etapa: Int) {
        _etapaQuestionario.value = etapa.coerceIn(0, 3)
    }

    fun atualizarRascunho(transform: (PerfilEmpresa) -> PerfilEmpresa) {
        _perfilRascunho.value = transform(_perfilRascunho.value)
    }

    fun salvarRascunhoEFinalizar() {
        val final = _perfilRascunho.value.copy(configurado = true)
        perfilRepository.salvarPerfil(final)
        _modoQuestionario.value = false

        val provedor = UfsaApplication.settings.atual().provedorIa
        val iaDisponivel = if (provedor == ProvedorIa.GEMINI_CLOUD) {
            perfilRepository.obterApiKeyAtual().isNotBlank()
        } else {
            UfsaApplication.gemma2bService.isDisponivel()
        }

        if (iaDisponivel && concursosAbertos.value.isNotEmpty()) {
            iniciarAnaliseIa()
        }
    }

    fun definirFiltroNivel(nivel: String?) {
        _filtroNivel.value = nivel
    }

    fun iniciarAnaliseIa() {
        val provedor = UfsaApplication.settings.atual().provedorIa
        val key = perfilRepository.obterApiKeyAtual()
        if (provedor == ProvedorIa.GEMINI_CLOUD && key.isBlank()) {
            _mensagemErro.value = appContext.getString(R.string.triagem_erro_key_ausente)
            return
        } else if (provedor == ProvedorIa.GEMMA_LOCAL && !UfsaApplication.gemma2bService.isDisponivel()) {
            _mensagemErro.value = appContext.getString(R.string.gemma_erro_sem_modelo)
            return
        }

        viewModelScope.launch {
            _mensagemErro.value = null
            var lista = concursosAbertos.value

            if (lista.isEmpty()) {
                _progressoAnalise.value = ProgressoAnalise(FaseAnalise.SINCRONIZANDO)
                try {
                    repository.sincronizarConcursos()
                } catch (_: Exception) {
                }
                lista = concursosAbertos.value
                if (lista.isEmpty()) {
                    _mensagemErro.value = appContext.getString(R.string.triagem_erro_sem_concursos)
                    _progressoAnalise.value = null
                    return@launch
                }
            }

            executarAnalise(key, lista)
        }
    }

    private suspend fun executarAnalise(key: String, lista: List<Concurso>) {
        _progressoAnalise.value = ProgressoAnalise(FaseAnalise.ANALISANDO, 0, lista.size.coerceAtMost(30))
        _mensagemErro.value = null
        try {
            val resultado = aiService.selecionarConcursos(
                apiKey = key,
                perfil = perfil.value,
                concursos = lista,
                onProgresso = { concluidos, total ->
                    _progressoAnalise.value = ProgressoAnalise(FaseAnalise.ANALISANDO, concluidos, total)
                }
            )
            resultado.fold(
                onSuccess = { recomendacoes ->
                    _progressoAnalise.value = ProgressoAnalise(FaseAnalise.GUARDANDO)
                    perfilRepository.salvarRecomendacoes(recomendacoes)
                    _mensagemErro.value = null
                },
                onFailure = { err ->
                    _mensagemErro.value = appContext.getString(R.string.triagem_erro_falha_analise, err.message ?: "")
                }
            )
        } catch (e: Exception) {
            _mensagemErro.value = appContext.getString(R.string.triagem_erro_inesperado, e.message ?: "")
        } finally {
            _progressoAnalise.value = null
        }
    }

    fun alternarGuardado(concurso: Concurso) {
        viewModelScope.launch {
            val guardadas = referenciasGuardadas.value
            if (guardadas.contains(concurso.referencia)) {
                repository.removerGuardado(concurso.referencia)
            } else {
                repository.guardar(concurso)
            }
        }
    }
}
