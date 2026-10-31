package mz.co.kevin.concursos.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.UfsaApplication
import mz.co.kevin.concursos.data.ai.Gemma2bService
import mz.co.kevin.concursos.data.ai.GemmaModelManager
import mz.co.kevin.concursos.data.ai.GemmaStatus
import mz.co.kevin.concursos.data.ai.GoogleAiService
import mz.co.kevin.concursos.data.repository.PerfilEmpresaRepository
import mz.co.kevin.concursos.data.settings.AppSettings
import mz.co.kevin.concursos.data.settings.ProvedorIa
import mz.co.kevin.concursos.data.settings.SettingsRepository
import mz.co.kevin.concursos.data.settings.TemaApp

import android.net.Uri

class DefinicoesViewModel(
    private val repo: SettingsRepository,
    private val perfilRepo: PerfilEmpresaRepository,
    private val aiService: GoogleAiService,
    private val gemmaModelManager: GemmaModelManager,
    private val gemma2bService: Gemma2bService,
    private val resolveString: (Int, Array<out Any?>) -> String,
) : ViewModel() {
    constructor() : this(
        UfsaApplication.settings,
        UfsaApplication.perfilRepository,
        UfsaApplication.googleAiService,
        UfsaApplication.gemmaModelManager,
        UfsaApplication.gemma2bService,
        { resId, args -> UfsaApplication.appContext.getString(resId, *args) },
    )

    private fun s(resId: Int): String = resolveString(resId, emptyArray())
    private fun s(resId: Int, vararg args: Any?): String = resolveString(resId, args)

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
    fun definirNotificarApenasRelevantesIa(ativo: Boolean) = repo.definirNotificarApenasRelevantesIa(ativo)
    fun definirIntervaloHoras(horas: Int) = repo.definirIntervaloHoras(horas)
    fun definirProvedorIa(provedor: ProvedorIa) = repo.definirProvedorIa(provedor)

    fun salvarApiKey(chave: String) {
        perfilRepo.salvarApiKey(chave)
        _statusValidacao.value = null
    }

    fun testarChave(chave: String) {
        viewModelScope.launch {
            _testando.value = true
            _statusValidacao.value = s(R.string.ia_a_testar)
            val resultado = aiService.testarChave(chave)
            resultado.fold(
                onSuccess = { msg ->
                    perfilRepo.salvarApiKey(chave)
                    _statusValidacao.value = s(R.string.ia_teste_ok, msg)
                },
                onFailure = { err ->
                    _statusValidacao.value = s(R.string.ia_teste_erro, err.message ?: s(R.string.ia_erro_validar_generico))
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
                    _statusTesteGemma.value = s(R.string.ia_inferencia_erro, err.message ?: s(R.string.val_desconhecido))
                }
            )
            _testandoGemma.value = false
        }
    }

    fun fecharTesteGemma() {
        _statusTesteGemma.value = null
    }
}
