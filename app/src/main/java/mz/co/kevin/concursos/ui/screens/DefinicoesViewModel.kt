package mz.co.kevin.concursos.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mz.co.kevin.concursos.UfsaApplication
import mz.co.kevin.concursos.data.settings.AppSettings
import mz.co.kevin.concursos.data.settings.TemaApp

class DefinicoesViewModel : ViewModel() {
    private val repo = UfsaApplication.settings
    private val perfilRepo = UfsaApplication.perfilRepository
    private val aiService = UfsaApplication.googleAiService

    val settings: StateFlow<AppSettings> = repo.settings
    val geminiApiKey: StateFlow<String> = perfilRepo.geminiApiKey

    private val _statusValidacao = MutableStateFlow<String?>(null)
    val statusValidacao: StateFlow<String?> = _statusValidacao.asStateFlow()

    private val _testando = MutableStateFlow(false)
    val testando: StateFlow<Boolean> = _testando.asStateFlow()

    fun definirTema(tema: TemaApp) = repo.definirTema(tema)
    fun definirCoresDinamicas(ativo: Boolean) = repo.definirCoresDinamicas(ativo)
    fun definirNotificacoesHabilitadas(ativo: Boolean) = repo.definirNotificacoesHabilitadas(ativo)
    fun definirNotificarApenasTI(ativo: Boolean) = repo.definirNotificarApenasTI(ativo)
    fun definirIntervaloHoras(horas: Int) = repo.definirIntervaloHoras(horas)

    fun salvarApiKey(chave: String) {
        perfilRepo.salvarApiKey(chave)
        _statusValidacao.value = null
    }

    fun testarChave(chave: String) {
        viewModelScope.launch {
            _testando.value = true
            _statusValidacao.value = "A testar conexão com o Google AI..."
            val resultado = aiService.testarChave(chave)
            resultado.fold(
                onSuccess = { msg ->
                    perfilRepo.salvarApiKey(chave)
                    _statusValidacao.value = "✓ $msg"
                },
                onFailure = { err ->
                    _statusValidacao.value = "✗ Erro: ${err.message ?: "Falha ao validar chave"}"
                }
            )
            _testando.value = false
        }
    }
}
