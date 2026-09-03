package mz.co.kevin.concursos

import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.PerfilEmpresa
import mz.co.kevin.concursos.data.model.PorteEmpresa
import mz.co.kevin.concursos.data.settings.AppSettings
import mz.co.kevin.concursos.data.settings.ProvedorIa
import mz.co.kevin.concursos.data.settings.TemaApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes puros (JVM) que fixam os contratos dos modelos de domínio: rótulos das
 * enums, valores por omissão dos data classes e as listas de referência.
 *
 * Os rótulos são agora recursos (@StringRes) resolvidos em runtime pela UI, pelo
 * que aqui apenas se verifica que cada entrada aponta para um recurso distinto.
 */
class ModelosTest {

    @Test
    fun categoriaConcurso_rotulos() {
        val ids = CategoriaConcurso.entries.map { it.labelRes }
        assertEquals(3, CategoriaConcurso.entries.size)
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { assertNotEquals(0, it) }
    }

    @Test
    fun porteEmpresa_rotulos() {
        val ids = PorteEmpresa.entries.map { it.labelRes }
        assertEquals(3, PorteEmpresa.entries.size)
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { assertNotEquals(0, it) }
    }

    @Test
    fun temaApp_rotulos() {
        val ids = TemaApp.entries.map { it.labelRes }
        assertEquals(3, TemaApp.entries.size)
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { assertNotEquals(0, it) }
    }

    @Test
    fun provedorIa_rotulos() {
        val ids = ProvedorIa.entries.map { it.labelRes }
        assertEquals(2, ProvedorIa.entries.size)
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { assertNotEquals(0, it) }
    }

    @Test
    fun appSettings_valores_por_omissao() {
        val s = AppSettings()
        assertEquals(TemaApp.SISTEMA, s.tema)
        assertTrue(s.coresDinamicas)
        assertTrue(s.notificacoesHabilitadas)
        assertEquals(4, s.intervaloHoras)
        assertEquals(ProvedorIa.GEMINI_CLOUD, s.provedorIa)
        assertEquals(listOf(4, 8, 12, 24), AppSettings.INTERVALOS_DISPONIVEIS)
        assertTrue(s.intervaloHoras in AppSettings.INTERVALOS_DISPONIVEIS)
    }

    @Test
    fun perfilEmpresa_valores_por_omissao() {
        val p = PerfilEmpresa()
        assertEquals("", p.nome)
        assertEquals(PorteEmpresa.MEDIA, p.porte)
        assertTrue(p.areasAtuacao.isEmpty())
        assertEquals(listOf("Todas as Províncias"), p.provinciasAtuacao)
        assertFalse(p.configurado)
    }

    @Test
    fun perfilEmpresa_listas_de_referencia() {
        assertEquals(8, PerfilEmpresa.DOCUMENTOS_PADRAO.size)
        assertEquals(13, PerfilEmpresa.AREAS_PADRAO.size)
        // 10 províncias + Maputo Cidade/Província + "Todas as Províncias"
        assertEquals(12, PerfilEmpresa.PROVINCIAS_MOCAMBIQUE.size)
        assertEquals("Todas as Províncias", PerfilEmpresa.PROVINCIAS_MOCAMBIQUE.first())
        assertTrue(PerfilEmpresa.PROVINCIAS_MOCAMBIQUE.contains("Niassa"))
    }

    @Test
    fun concurso_preenche_timestampCaptura_automaticamente() {
        val antes = System.currentTimeMillis()
        val c = Concurso(
            referencia = "R1",
            modalidade = "Concurso Público",
            objecto = "Objecto",
            ugea = "UGEA",
            provincia = "Maputo",
            dataLancamento = "2026-01-01",
            dataAbertura = "2026-02-01",
            linkDetalhes = "",
            categoria = CategoriaConcurso.ABERTO,
            ehInformatica = false
        )
        assertTrue(c.timestampCaptura >= antes)
        assertTrue(c.timestampCaptura <= System.currentTimeMillis())
    }
}
