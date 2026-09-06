package mz.co.kevin.concursos.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.StringRes
import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.UfsaApplication
import mz.co.kevin.concursos.data.model.DetalhesFornecedorCef
import mz.co.kevin.concursos.data.model.FornecedorCef
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OrdemFornecedor(@StringRes val rotuloRes: Int) {
    NOME(R.string.ordem_nome),
    DATA_RECENTE(R.string.ordem_data_recente)
}

data class FornecedoresUiState(
    val provinciaSelecionada: String = "",
    val anoInscricao: String = "",
    val ordem: OrdemFornecedor = OrdemFornecedor.NOME,
    val termoPesquisa: String = "",
    val carregando: Boolean = false,
    val erro: String? = null
) {
    val filtrosActivos: Int
        get() = (if (provinciaSelecionada.isNotEmpty()) 1 else 0) +
            (if (anoInscricao.isNotEmpty()) 1 else 0) +
            (if (ordem != OrdemFornecedor.NOME) 1 else 0)
}

/** Extrai o ano ("2020") de datas "dd/MM/yyyy" ou "yyyy-MM-dd". */
internal fun anoDaData(data: String): String =
    Regex("(19|20)\\d{2}").findAll(data).lastOrNull()?.value ?: ""

/** Chave ordenável "yyyyMMdd" a partir de "dd/MM/yyyy" ou "yyyy-MM-dd"; "" se não parsear. */
internal fun chaveDataInscricao(data: String): String {
    val d = data.trim()
    Regex("^(\\d{2})/(\\d{2})/(\\d{4})$").find(d)?.destructured?.let { (dia, mes, ano) ->
        return "$ano$mes$dia"
    }
    Regex("^(\\d{4})-(\\d{2})-(\\d{2})$").find(d)?.destructured?.let { (ano, mes, dia) ->
        return "$ano$mes$dia"
    }
    return ""
}

@OptIn(ExperimentalCoroutinesApi::class)
class FornecedoresViewModel : ViewModel() {
    private val repo = UfsaApplication.repository
    private val _state = MutableStateFlow(FornecedoresUiState())
    val state: StateFlow<FornecedoresUiState> = _state.asStateFlow()

    val fornecedores: StateFlow<List<FornecedorCef>> = _state
        .flatMapLatest { s ->
            repo.observarFornecedores(s.provinciaSelecionada, s.termoPesquisa)
                .map { lista ->
                    val filtrada = if (s.anoInscricao.isBlank()) lista
                    else lista.filter { anoDaData(it.dataInscricao) == s.anoInscricao }
                    when (s.ordem) {
                        OrdemFornecedor.NOME -> filtrada.sortedBy { it.nome.lowercase() }
                        OrdemFornecedor.DATA_RECENTE ->
                            filtrada.sortedByDescending { chaveDataInscricao(it.dataInscricao) }
                    }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Províncias distintas presentes nos fornecedores já carregados (para o filtro). */
    val provincias: StateFlow<List<String>> = repo.observarProvinciasFornecedores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Anos de inscrição distintos presentes nos dados, mais recente primeiro. */
    val anosInscricao: StateFlow<List<String>> = repo.observarFornecedores("", "")
        .map { lista ->
            lista.mapNotNull { anoDaData(it.dataInscricao).ifBlank { null } }
                .distinct()
                .sortedDescending()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Carrega todos os concorrentes ao abrir (params vazios = lista completa do portal).
        pesquisarRemoto()
    }

    fun alterarPesquisa(termo: String) {
        _state.update { it.copy(termoPesquisa = termo) }
    }

    fun alterarProvincia(prov: String) {
        _state.update { it.copy(provinciaSelecionada = prov) }
    }

    fun alterarAno(ano: String) {
        _state.update { it.copy(anoInscricao = ano) }
    }

    fun alterarOrdem(ordem: OrdemFornecedor) {
        _state.update { it.copy(ordem = ordem) }
    }

    fun limparFiltros() {
        _state.update {
            it.copy(provinciaSelecionada = "", anoInscricao = "", ordem = OrdemFornecedor.NOME)
        }
    }

    fun pesquisarRemoto() {
        viewModelScope.launch {
            _state.update { it.copy(carregando = true, erro = null) }
            try {
                repo.sincronizarFornecedores(_state.value.provinciaSelecionada, _state.value.termoPesquisa)
            } catch (e: Exception) {
                _state.update { it.copy(erro = e.message ?: UfsaApplication.appContext.getString(R.string.erro_buscar_fornecedores)) }
            } finally {
                _state.update { it.copy(carregando = false) }
            }
        }
    }

    /**
     * Busca sob demanda a ficha detalhada (ramos, contactos, regime) de um
     * fornecedor. Falha com [Result.failure] quando o portal está indisponível.
     */
    suspend fun carregarDetalhesFornecedor(f: FornecedorCef): Result<DetalhesFornecedorCef> =
        runCatching { repo.obterDetalhesFornecedor(f.certificado, f.linkDetalhes) }
}
