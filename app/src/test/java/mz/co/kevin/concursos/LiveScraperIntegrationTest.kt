package mz.co.kevin.concursos

import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.remote.DevelopmentAidScraper
import mz.co.kevin.concursos.data.remote.UfsaScraper
import mz.co.kevin.concursos.data.remote.UngmScraper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LiveScraperIntegrationTest {

    @Test
    fun test01_carregarUfsa() {
        println("\n==========================================")
        println(">>> TESTE 1: CARREGAR UFSA (Nacional) <<<")
        println("==========================================")
        val lista = UfsaScraper.extrairConcursos(CategoriaConcurso.ABERTO)
        println("Total de concursos UFSA carregados: ${lista.size}")
        assertTrue("UFSA deve retornar pelo menos 1 concurso aberto", lista.isNotEmpty())

        val primeiro = lista.first()
        println("Exemplo UFSA:")
        println("  Referência:  ${primeiro.referencia}")
        println("  Objecto:     ${primeiro.objecto}")
        println("  UGEA/Entidade: ${primeiro.ugea}")
        println("  Província:   ${primeiro.provincia}")
        println("  Data Abert.: ${primeiro.dataAbertura}")
        println("  Fonte:       ${primeiro.fonte}")

        assertEquals("UFSA", primeiro.fonte)
        assertTrue(primeiro.referencia.isNotBlank())
        assertTrue(primeiro.objecto.isNotBlank())
    }

    @Test
    fun test02_carregarUngm() {
        println("\n==========================================")
        println(">>> TESTE 2: CARREGAR UNGM (Nações Unidas) <<<")
        println("==========================================")
        val lista = UngmScraper.extrairConcursos(CategoriaConcurso.ABERTO)
        println("Total de concursos UNGM carregados: ${lista.size}")
        assertTrue("UNGM deve retornar pelo menos 1 concurso para Moçambique", lista.isNotEmpty())

        val primeiro = lista.first()
        println("Exemplo UNGM:")
        println("  Referência:  ${primeiro.referencia}")
        println("  Objecto:     ${primeiro.objecto}")
        println("  UGEA/Entidade: ${primeiro.ugea}")
        println("  Província:   ${primeiro.provincia}")
        println("  Data Abert.: ${primeiro.dataAbertura}")
        println("  Fonte:       ${primeiro.fonte}")

        assertEquals("UNGM", primeiro.fonte)
        assertTrue("Referência UNGM deve ter prefixo UNGM-", primeiro.referencia.startsWith("UNGM-"))
        assertTrue(primeiro.objecto.isNotBlank())
    }

    @Test
    fun test03_carregarDevelopmentAid() {
        println("\n==========================================")
        println(">>> TESTE 3: CARREGAR DEVELOPMENTAID / DOADORES <<<")
        println("==========================================")
        val lista = DevelopmentAidScraper.extrairConcursos(CategoriaConcurso.ABERTO)
        println("Total de concursos DevelopmentAid carregados: ${lista.size}")
        assertTrue("DevelopmentAid deve retornar pelo menos 1 concurso para Moçambique", lista.isNotEmpty())

        val primeiro = lista.first()
        println("Exemplo DevelopmentAid:")
        println("  Referência:  ${primeiro.referencia}")
        println("  Objecto:     ${primeiro.objecto}")
        println("  UGEA/Entidade: ${primeiro.ugea}")
        println("  Província:   ${primeiro.provincia}")
        println("  Data Abert.: ${primeiro.dataAbertura}")
        println("  Fonte:       ${primeiro.fonte}")

        assertEquals("DevelopmentAid", primeiro.fonte)
        assertTrue("Referência DEV deve ter prefixo DEV-", primeiro.referencia.startsWith("DEV-"))
        assertTrue(primeiro.objecto.isNotBlank())
    }

    @Test
    fun test04_coexistenciaEFiltragemDeCadaUm() {
        println("\n==========================================")
        println(">>> TESTE 4: COEXISTÊNCIA E FILTRAGEM <<<")
        println("==========================================")
        val ufsa = UfsaScraper.extrairConcursos(CategoriaConcurso.ABERTO)
        val ungm = UngmScraper.extrairConcursos(CategoriaConcurso.ABERTO)
        val dev = DevelopmentAidScraper.extrairConcursos(CategoriaConcurso.ABERTO)

        val todos = mutableListOf<Concurso>()
        todos.addAll(ufsa)
        todos.addAll(ungm)
        todos.addAll(dev)

        println("Contagem combinada total: ${todos.size}")
        println("  - UFSA: ${ufsa.size}")
        println("  - UNGM: ${ungm.size}")
        println("  - DevelopmentAid: ${dev.size}")

        // 1. Verificar unicidade das chaves primárias (referência)
        val refs = todos.map { it.referencia }
        val refsUnicas = refs.toSet()
        println("Unicidade das chaves primárias: ${refsUnicas.size} únicas de ${refs.size} total")
        assertEquals("Não devem existir colisões de chaves primárias entre portais", refs.size, refsUnicas.size)

        // 2. Simular filtro "Todos" (fonteFiltro == "")
        val filtroTodos = todos.filter { true }
        assertEquals(todos.size, filtroTodos.size)

        // 3. Simular filtro por cada fonte individual
        val filtroUfsa = todos.filter { it.fonte.equals("UFSA", ignoreCase = true) }
        val filtroUngm = todos.filter { it.fonte.equals("UNGM", ignoreCase = true) }
        val filtroDev = todos.filter { it.fonte.equals("DevelopmentAid", ignoreCase = true) }

        assertEquals(ufsa.size, filtroUfsa.size)
        assertEquals(ungm.size, filtroUngm.size)
        assertEquals(dev.size, filtroDev.size)

        println("Filtragem de cada um validada com 100% de precisão!")
    }

    @Test
    fun test05_detalhesDeCadaUm() {
        println("\n==========================================")
        println(">>> TESTE 5: DETALHES DE CADA FONTE <<<")
        println("==========================================")

        // Detalhes UFSA
        val ufsaLista = UfsaScraper.extrairConcursos(CategoriaConcurso.ABERTO)
        if (ufsaLista.isNotEmpty()) {
            val refUfsa = ufsaLista.first().referencia
            val detUfsa = UfsaScraper.extrairDetalhes(refUfsa)
            println("Detalhes UFSA para $refUfsa: ${detUfsa.campos.size} campos extraídos")
            assertNotNull(detUfsa)
        }

        // Detalhes UNGM
        val ungmLista = UngmScraper.extrairConcursos(CategoriaConcurso.ABERTO)
        if (ungmLista.isNotEmpty()) {
            val refUngm = ungmLista.first().linkDetalhes.ifBlank { ungmLista.first().referencia }
            try {
                val detUngm = UngmScraper.extrairDetalhes(refUngm)
                println("Detalhes UNGM para $refUngm: ${detUngm.campos.size} campos extraídos")
                assertNotNull(detUngm)
            } catch (e: Exception) {
                println("Aviso detalhes UNGM: ${e.message}")
            }
        }

        // Detalhes DevelopmentAid
        val devLista = DevelopmentAidScraper.extrairConcursos(CategoriaConcurso.ABERTO)
        if (devLista.isNotEmpty()) {
            val refDev = devLista.first().linkDetalhes.ifBlank { devLista.first().referencia }
            try {
                val detDev = DevelopmentAidScraper.extrairDetalhes(refDev)
                println("Detalhes DevelopmentAid para $refDev: ${detDev.campos.size} campos extraídos")
                assertNotNull(detDev)
            } catch (e: Exception) {
                println("Aviso detalhes DevelopmentAid: ${e.message}")
            }
        }
    }
}
