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

import android.net.Uri
import mz.co.kevin.concursos.data.ai.GemmaStatus
import mz.co.kevin.concursos.data.settings.ProvedorIa

class DefinicoesViewModel : ViewModel() {
    private val repo = UfsaApplication.settings
    private val perfilRepo = UfsaApplication.perfilRepository
    private val aiService = UfsaApplication.googleAiService
    private val gemmaModelManager = UfsaApplication.gemmaModelManager
    private val gemma2bService = UfsaApplication.gemma2bService

    val settings: StateFlow<AppSettings> = repo.settings
    val geminiApiKey: StateFlow<String> = perfilRepo.geminiApiKey
    val gemmaStatus: StateFlow<GemmaStatus> = gemmaModelManager.status

    private val _statusValidacao = MutableStateFlow<String?>(null)
    val statusValidacao: StateFlow<String?> = _statusValidacao.asStateFlow()

    private val _testando = MutableStateFlow(false)
    val testando: StateFlow<Boolean> = _testando.asStateFlow()

    private val _statusTesteGemma = MutableStateFlow<String?>(null)
    val statusTesteGemma: StateFlow<String?> = _statusTesteGemma.asStateFlow()

    private val _testandoGemma = MutableStateFlow(false)
    val testandoGemma: StateFlow<Boolean> = _testandoGemma.asStateFlow()

    fun definirTema(tema: TemaApp) = repo.definirTema(tema)
    fun definirCoresDinamicas(ativo: Boolean) = repo.definirCoresDinamicas(ativo)
    fun definirNotificacoesHabilitadas(ativo: Boolean) = repo.definirNotificacoesHabilitadas(ativo)
    fun definirNotificarApenasTI(ativo: Boolean) = repo.definirNotificarApenasTI(ativo)
    fun definirIntervaloHoras(horas: Int) = repo.definirIntervaloHoras(horas)
    fun definirProvedorIa(provedor: ProvedorIa) = repo.definirProvedorIa(provedor)

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

    fun iniciarDownloadGemma(url: String? = null) {
        if (!url.isNullOrBlank()) {
            gemmaModelManager.iniciarDownload(url.trim())
        } else {
            gemmaModelManager.iniciarDownload()
        }
    }

    fun cancelarDownloadGemma() {
        gemmaModelManager.cancelarDownload()
    }

    fun importarModeloGemma(uri: Uri) {
        viewModelScope.launch {
            gemmaModelManager.importarFicheiro(uri)
        }
    }

    fun eliminarModeloGemma() {
        gemma2bService.liberarMemoria()
        gemmaModelManager.eliminarModelo()
        _statusTesteGemma.value = null
    }

    fun testarGemma() {
        viewModelScope.launch {
            _testandoGemma.value = true
            _statusTesteGemma.value = null
            val resultado = gemma2bService.testarInferencia()
            resultado.fold(
                onSuccess = { resposta ->
                    _statusTesteGemma.value = resposta
                },
                onFailure = { err ->
                    _statusTesteGemma.value = "Erro na inferência: ${err.message ?: "Desconhecido"}"
                }
            )
            _testandoGemma.value = false
        }
    }

    fun fecharTesteGemma() {
        _statusTesteGemma.value = null
    }
}
