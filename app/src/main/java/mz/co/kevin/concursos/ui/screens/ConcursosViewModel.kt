package mz.co.kevin.concursos.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.StringRes
import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.UfsaApplication
import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.model.Concurso
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

import mz.co.kevin.concursos.data.ai.GoogleAiService
import kotlinx.coroutines.flow.combine

enum class SecaoConcursos(@StringRes val labelRes: Int) {
    ABERTOS(R.string.secao_abertos),
    GUARDADOS(R.string.secao_guardados),
    ADJUDICADOS(R.string.secao_adjudicados),
    CANCELADOS(R.string.secao_cancelados)
}

data class ConcursosUiState(
    val secao: SecaoConcursos = SecaoConcursos.ABERTOS,
    val categoria: CategoriaConcurso = CategoriaConcurso.ABERTO,
    val provinciaFiltro: String = "",
    val categoriaIaFiltro: String = "",
    val apenasTiFiltro: Boolean = false,
    /** Data-limite ISO `yyyy-MM-dd`; só concursos que abrem até esta data. */
    val prazoAntesDe: String = "",
    /** Texto bruto do filtro em linguagem natural (mantido para o campo do sheet). */
    val filtroNaturalTexto: String = "",
    val termoPesquisa: String = "",
    val carregando: Boolean = false,
    val erro: String? = null
) {
    /** Número de filtros activos (para o badge do botão de filtros). */
    val filtrosActivos: Int
        get() = (if (provinciaFiltro.isNotEmpty()) 1 else 0) +
            (if (categoriaIaFiltro.isNotEmpty()) 1 else 0) +
            (if (apenasTiFiltro) 1 else 0) +
            (if (prazoAntesDe.isNotEmpty()) 1 else 0)
}

@OptIn(ExperimentalCoroutinesApi::class)
class ConcursosViewModel : ViewModel() {
    private val repo = UfsaApplication.repository
    private val aiService = UfsaApplication.googleAiService
    private val _state = MutableStateFlow(ConcursosUiState())
    val state: StateFlow<ConcursosUiState> = _state.asStateFlow()

    private val _mapaCategoriasIa = MutableStateFlow<Map<String, String>>(emptyMap())
    val mapaCategoriasIa: StateFlow<Map<String, String>> = _mapaCategoriasIa.asStateFlow()

    private val _categorizandoComIa = MutableStateFlow(false)
    val categorizandoComIa: StateFlow<Boolean> = _categorizandoComIa.asStateFlow()

    private val _interpretandoFiltro = MutableStateFlow(false)
    val interpretandoFiltro: StateFlow<Boolean> = _interpretandoFiltro.asStateFlow()

    val categoriasIaDisponiveis: StateFlow<List<String>> = _mapaCategoriasIa
        .map { mapa -> mapa.values.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val listaConcursos: StateFlow<List<Concurso>> = combine(
        _state,
        _mapaCategoriasIa
    ) { s, mapaIa ->
        Pair(s, mapaIa)
    }.flatMapLatest { (s, mapaIa) ->
        val baseFlow = when (s.secao) {
            SecaoConcursos.GUARDADOS -> {
                repo.observarGuardados().map { guardadosList ->
                    guardadosList.map { g ->
                        Concurso(
                            referencia = g.referencia,
                            modalidade = g.modalidade,
                            objecto = g.objecto,
                            ugea = g.ugea,
                            provincia = g.provincia,
                            dataLancamento = g.dataInicioSubmissao,
                            dataAbertura = g.dataFimSubmissao,
                            linkDetalhes = g.linkDetalhes,
                            categoria = g.categoria,
                            ehInformatica = g.ehInformatica
                        )
                    }.filter { c ->
                        val matchProv = s.provinciaFiltro.isEmpty() || c.provincia.equals(s.provinciaFiltro, ignoreCase = true)
                        val matchBusca = s.termoPesquisa.isEmpty() ||
                            c.objecto.contains(s.termoPesquisa, ignoreCase = true) ||
                            c.ugea.contains(s.termoPesquisa, ignoreCase = true) ||
                            c.referencia.contains(s.termoPesquisa, ignoreCase = true)
                        matchProv && matchBusca
                    }
                }
            }
            SecaoConcursos.ABERTOS -> repo.observarConcursos(CategoriaConcurso.ABERTO, s.provinciaFiltro, s.termoPesquisa)
            SecaoConcursos.ADJUDICADOS -> repo.observarConcursos(CategoriaConcurso.ADJUDICADO, s.provinciaFiltro, s.termoPesquisa)
            SecaoConcursos.CANCELADOS -> repo.observarConcursos(CategoriaConcurso.CANCELADO, s.provinciaFiltro, s.termoPesquisa)
        }

        baseFlow.map { lista ->
            lista.filter { c ->
                val matchIa = s.categoriaIaFiltro.isEmpty() ||
                    mapaIa[c.referencia] == s.categoriaIaFiltro
                val matchTi = !s.apenasTiFiltro || c.ehInformatica
                val matchPrazo = s.prazoAntesDe.isEmpty() ||
                    (c.dataAbertura.length >= 10 && c.dataAbertura.take(10) <= s.prazoAntesDe)
                matchIa && matchTi && matchPrazo
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val provincias: StateFlow<List<String>> = _state
        .flatMapLatest { s ->
            when (s.secao) {
                SecaoConcursos.GUARDADOS -> repo.observarGuardados().map { list ->
                    list.map { it.provincia.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
                }
                SecaoConcursos.ABERTOS -> repo.observarProvincias(CategoriaConcurso.ABERTO)
                SecaoConcursos.ADJUDICADOS -> repo.observarProvincias(CategoriaConcurso.ADJUDICADO)
                SecaoConcursos.CANCELADOS -> repo.observarProvincias(CategoriaConcurso.CANCELADO)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val referenciasGuardadas: StateFlow<Set<String>> = repo.observarReferenciasGuardadas()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        atualizar()
    }

    fun mudarSecao(secao: SecaoConcursos) {
        val cat = when (secao) {
            SecaoConcursos.ABERTOS -> CategoriaConcurso.ABERTO
            SecaoConcursos.ADJUDICADOS -> CategoriaConcurso.ADJUDICADO
            SecaoConcursos.CANCELADOS -> CategoriaConcurso.CANCELADO
            SecaoConcursos.GUARDADOS -> CategoriaConcurso.ABERTO
        }
        _state.update {
            it.copy(
                secao = secao,
                categoria = cat,
                provinciaFiltro = "",
                categoriaIaFiltro = "",
                apenasTiFiltro = false,
                prazoAntesDe = "",
                filtroNaturalTexto = "",
            )
        }
    }

    fun mudarCategoria(cat: CategoriaConcurso) {
        val secao = when (cat) {
            CategoriaConcurso.ABERTO -> SecaoConcursos.ABERTOS
            CategoriaConcurso.ADJUDICADO -> SecaoConcursos.ADJUDICADOS
            CategoriaConcurso.CANCELADO -> SecaoConcursos.CANCELADOS
        }
        mudarSecao(secao)
    }

    fun mudarProvincia(prov: String) {
        _state.update { it.copy(provinciaFiltro = if (it.provinciaFiltro == prov) "" else prov) }
    }

    fun mudarCategoriaIa(cat: String) {
        _state.update { it.copy(categoriaIaFiltro = if (it.categoriaIaFiltro == cat) "" else cat) }
    }

    fun mudarApenasTi(activo: Boolean) {
        _state.update { it.copy(apenasTiFiltro = activo) }
    }

    fun mudarPrazoAntesDe(iso: String) {
        _state.update { it.copy(prazoAntesDe = if (it.prazoAntesDe == iso) "" else iso) }
    }

    fun mudarFiltroNatural(texto: String) {
        _state.update { it.copy(filtroNaturalTexto = texto) }
    }

    /**
     * Interpreta [texto] (o campo de linguagem natural) e aplica o filtro
     * resultante ao estado. Usa IA quando disponível, senão o parser offline —
     * ver [GoogleAiService.interpretarFiltro].
     */
    fun aplicarFiltroNatural(texto: String) {
        if (texto.isBlank() || _interpretandoFiltro.value) return
        val apiKey = UfsaApplication.perfilRepository.geminiApiKey.value
        val provs = provincias.value
        val cats = categoriasIaDisponiveis.value
        viewModelScope.launch {
            _interpretandoFiltro.value = true
            try {
                val f = aiService.interpretarFiltro(apiKey, texto, provs, cats).getOrNull()
                    ?: return@launch
                _state.update {
                    it.copy(
                        filtroNaturalTexto = texto,
                        provinciaFiltro = f.provincia ?: it.provinciaFiltro,
                        categoriaIaFiltro = f.categoriaIa ?: it.categoriaIaFiltro,
                        apenasTiFiltro = f.apenasTi || it.apenasTiFiltro,
                        prazoAntesDe = f.prazoAntesDe ?: it.prazoAntesDe,
                        termoPesquisa = f.termo ?: it.termoPesquisa,
                    )
                }
            } finally {
                _interpretandoFiltro.value = false
            }
        }
    }

    fun limparFiltros() {
        _state.update {
            it.copy(
                provinciaFiltro = "",
                categoriaIaFiltro = "",
                apenasTiFiltro = false,
                prazoAntesDe = "",
                filtroNaturalTexto = "",
                termoPesquisa = "",
            )
        }
    }

    fun classificarConcursosComIa() {
        val apiKey = UfsaApplication.perfilRepository.geminiApiKey.value
        if (apiKey.isBlank() || _categorizandoComIa.value) return

        val concursosAtuais = listaConcursos.value
        if (concursosAtuais.isEmpty()) return

        val naoCategorizados = concursosAtuais.filter { it.referencia !in _mapaCategoriasIa.value }
        if (naoCategorizados.isEmpty()) return

        viewModelScope.launch {
            _categorizandoComIa.value = true
            try {
                val res = aiService.categorizarConcursos(apiKey, naoCategorizados)
                res.onSuccess { novoMapa ->
                    _mapaCategoriasIa.update { it + novoMapa }
                }
            } catch (_: Exception) {
            } finally {
                _categorizandoComIa.value = false
            }
        }
    }

    fun mudarPesquisa(termo: String) {
        _state.update { it.copy(termoPesquisa = termo) }
    }

    fun alternarGuardado(concurso: Concurso) {
        viewModelScope.launch {
            if (concurso.referencia in referenciasGuardadas.value) {
                repo.removerGuardado(concurso.referencia)
            } else {
                repo.guardar(concurso)
            }
        }
    }

    fun atualizar() {
        viewModelScope.launch {
            _state.update { it.copy(carregando = true, erro = null) }
            try {
                repo.sincronizarConcursos(marcarComoVistos = true)
                // Se o usuário tiver chave de IA configurada, categoriza os novos concursos automaticamente
                val apiKey = UfsaApplication.perfilRepository.geminiApiKey.value
                if (apiKey.isNotBlank()) {
                    classificarConcursosComIa()
                }
            } catch (e: Exception) {
                _state.update { it.copy(erro = e.message ?: UfsaApplication.appContext.getString(R.string.erro_atualizar_concursos)) }
            } finally {
                _state.update { it.copy(carregando = false) }
            }
        }
    }
}
