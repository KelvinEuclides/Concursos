package mz.co.kevin.concursos.data.repository

import mz.co.kevin.concursos.data.local.ConcursoDao
import mz.co.kevin.concursos.data.local.FornecedorDao
import mz.co.kevin.concursos.data.local.GuardadosDao
import mz.co.kevin.concursos.data.local.RefsVistasDao
import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.ConcursoGuardado
import mz.co.kevin.concursos.data.model.DetalhesConcurso
import mz.co.kevin.concursos.data.model.FornecedorCef
import mz.co.kevin.concursos.data.model.RefVista
import mz.co.kevin.concursos.data.remote.UfsaScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class UfsaRepository(
    private val concursoDao: ConcursoDao,
    private val fornecedorDao: FornecedorDao,
    private val guardadosDao: GuardadosDao,
    private val refsVistasDao: RefsVistasDao
) {
    private companion object {
        /** Limite do histórico de referências já vistas. */
        const val MAX_REFS_VISTAS = 4000
    }

    fun observarConcursos(
        cat: CategoriaConcurso,
        provincia: String,
        busca: String
    ): Flow<List<Concurso>> =
        concursoDao.observarConcursos(cat, provincia, busca)

    fun observarProvincias(cat: CategoriaConcurso): Flow<List<String>> =
        concursoDao.observarProvincias(cat)

    fun observarFornecedores(prov: String, busca: String): Flow<List<FornecedorCef>> =
        fornecedorDao.observarFornecedores(prov, busca)

    // ---- Concursos guardados ----

    fun observarGuardados(): Flow<List<ConcursoGuardado>> = guardadosDao.observarGuardados()

    fun observarReferenciasGuardadas(): Flow<List<String>> = guardadosDao.observarReferencias()

    suspend fun guardar(concurso: Concurso) = withContext(Dispatchers.IO) {
        guardadosDao.guardar(
            ConcursoGuardado(
                referencia = concurso.referencia,
                modalidade = concurso.modalidade,
                objecto = concurso.objecto,
                ugea = concurso.ugea,
                provincia = concurso.provincia,
                categoria = concurso.categoria,
                ehInformatica = concurso.ehInformatica,
                dataInicioSubmissao = concurso.dataLancamento,
                dataFimSubmissao = concurso.dataAbertura,
                linkDetalhes = concurso.linkDetalhes
            )
        )
    }

    suspend fun removerGuardado(referencia: String) = withContext(Dispatchers.IO) {
        guardadosDao.remover(referencia)
    }

    suspend fun atualizarRequisitosGuardado(referencia: String, requisitos: String) =
        withContext(Dispatchers.IO) {
            guardadosDao.atualizarRequisitos(referencia, requisitos)
        }

    // ---- Referências já vistas (para não repetir notificações) ----

    /**
     * Marca referências como já vistas. Idempotente (ignora duplicados) e poda o
     * histórico para não crescer indefinidamente.
     */
    suspend fun marcarConcursosVistos(referencias: List<String>) = withContext(Dispatchers.IO) {
        if (referencias.isEmpty()) return@withContext
        refsVistasDao.marcar(referencias.map { RefVista(it) })
        refsVistasDao.podar(MAX_REFS_VISTAS)
    }

    suspend fun marcarConcursoVisto(referencia: String) = marcarConcursosVistos(listOf(referencia))

    // ---- Sincronização ----

    /**
     * Sincroniza todas as categorias e devolve os concursos **ainda não vistos**
     * (comparados com o registo persistente `refs_vistas`, que sobrevive à
     * reescrita da tabela `concursos`).
     *
     * @param marcarComoVistos quando `true` (uso em primeiro plano — o utilizador
     *   está a ver a app), regista imediatamente tudo o que foi obtido como visto,
     *   para o worker não voltar a notificar esses concursos.
     */
    suspend fun sincronizarConcursos(marcarComoVistos: Boolean = false): List<Concurso> =
        withContext(Dispatchers.IO) {
            val vistos = refsVistasDao.todas().toSet()
            val novos = mutableListOf<Concurso>()
            val obtidos = mutableListOf<String>()

            for (cat in CategoriaConcurso.entries) {
                try {
                    val remotos = UfsaScraper.extrairConcursos(cat)
                    remotos.forEach { c ->
                        obtidos.add(c.referencia)
                        if (c.referencia !in vistos) novos.add(c)
                    }
                    if (remotos.isNotEmpty()) {
                        concursoDao.inserirOuAtualizar(remotos)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (marcarComoVistos && obtidos.isNotEmpty()) {
                marcarConcursosVistos(obtidos)
            }
            novos
        }

    suspend fun sincronizarFornecedores(provincia: String, termo: String) = withContext(Dispatchers.IO) {
        val remotos = UfsaScraper.buscarFornecedoresCEF(provincia, termo)
        if (remotos.isNotEmpty()) {
            fornecedorDao.salvarFornecedores(remotos)
        }
    }

    suspend fun obterDetalhes(referencia: String): DetalhesConcurso = withContext(Dispatchers.IO) {
        UfsaScraper.extrairDetalhes(referencia)
    }
}
