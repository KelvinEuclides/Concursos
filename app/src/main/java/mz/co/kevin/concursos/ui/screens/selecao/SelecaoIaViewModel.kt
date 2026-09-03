package mz.co.kevin.concursos.ui.screens.selecao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mz.co.kevin.concursos.UfsaApplication
import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.PerfilEmpresa
import mz.co.kevin.concursos.data.model.RecomendacaoConcursoIa

class SelecaoIaViewModel : ViewModel() {
    private val repository = UfsaApplication.repository
    private val perfilRepository = UfsaApplication.perfilRepository
    private val aiService = UfsaApplication.googleAiService

    val perfil: StateFlow<PerfilEmpresa> = perfilRepository.perfil
    val geminiApiKey: StateFlow<String> = perfilRepository.geminiApiKey
    val recomendacoes: StateFlow<List<RecomendacaoConcursoIa>> = perfilRepository.recomendacoes

    val concursosAbertos: StateFlow<List<Concurso>> = repository.observarConcursos(
        cat = CategoriaConcurso.ABERTO,
        apenasTI = false,
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

    private val _analisando = MutableStateFlow(false)
    val analisando: StateFlow<Boolean> = _analisando.asStateFlow()

    private val _filtroNivel = MutableStateFlow<String?>(null)
    val filtroNivel: StateFlow<String?> = _filtroNivel.asStateFlow()

    private val _mensagemErro = MutableStateFlow<String?>(null)
    val mensagemErro: StateFlow<String?> = _mensagemErro.asStateFlow()

    private val _statusChave = MutableStateFlow<String?>(null)
    val statusChave: StateFlow<String?> = _statusChave.asStateFlow()

    private val _testandoChave = MutableStateFlow(false)
    val testandoChave: StateFlow<Boolean> = _testandoChave.asStateFlow()

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
        _etapaQuestionario.value = etapa.coerceIn(0, 4)
    }

    fun atualizarRascunho(transform: (PerfilEmpresa) -> PerfilEmpresa) {
        _perfilRascunho.value = transform(_perfilRascunho.value)
    }

    fun salvarRascunhoEFinalizar() {
        val final = _perfilRascunho.value.copy(configurado = true)
        perfilRepository.salvarPerfil(final)
        _modoQuestionario.value = false

        // Se houver chave configurada e concursos, disparar análise
        if (perfilRepository.obterApiKeyAtual().isNotBlank() && concursosAbertos.value.isNotEmpty()) {
            iniciarAnaliseIa()
        }
    }

    fun salvarApiKey(key: String) {
        perfilRepository.salvarApiKey(key)
        _statusChave.value = null
    }

    fun testarChave(chave: String) {
        viewModelScope.launch {
            _testandoChave.value = true
            _statusChave.value = "A testar conexão com o Google AI..."
            val res = aiService.testarChave(chave)
            res.fold(
                onSuccess = { msg ->
                    perfilRepository.salvarApiKey(chave)
                    _statusChave.value = "✓ $msg"
                },
                onFailure = { err ->
                    _statusChave.value = "✗ Erro: ${err.message ?: "Falha ao validar chave"}"
                }
            )
            _testandoChave.value = false
        }
    }

    fun definirFiltroNivel(nivel: String?) {
        _filtroNivel.value = nivel
    }

    fun iniciarAnaliseIa() {
        val key = perfilRepository.obterApiKeyAtual()
        if (key.isBlank()) {
            _mensagemErro.value = "Por favor configure a sua chave Google AI (Gemini) antes de analisar."
            return
        }

        val lista = concursosAbertos.value
        if (lista.isEmpty()) {
            // Tentar sincronizar concursos primeiro se o banco estiver vazio
            viewModelScope.launch {
                _analisando.value = true
                _mensagemErro.value = null
                try {
                    repository.sincronizarConcursos()
                } catch (_: Exception) {}

                val novos = concursosAbertos.value
                if (novos.isEmpty()) {
                    _mensagemErro.value = "Nenhum concurso aberto encontrado no portal da UFSA no momento."
                    _analisando.value = false
                    return@launch
                }

                executarAnalise(key, novos)
            }
            return
        }

        viewModelScope.launch {
            executarAnalise(key, lista)
        }
    }

    private suspend fun executarAnalise(key: String, lista: List<Concurso>) {
        _analisando.value = true
        _mensagemErro.value = null
        try {
            val resultado = aiService.selecionarConcursos(key, perfil.value, lista)
            resultado.fold(
                onSuccess = { recomendacoes ->
                    perfilRepository.salvarRecomendacoes(recomendacoes)
                    _mensagemErro.value = null
                },
                onFailure = { err ->
                    _mensagemErro.value = "Falha na análise da IA: ${err.message}"
                }
            )
        } catch (e: Exception) {
            _mensagemErro.value = "Erro inesperado: ${e.message}"
        } finally {
            _analisando.value = false
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
