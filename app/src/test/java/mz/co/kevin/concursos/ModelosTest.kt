package mz.co.kevin.concursos

import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.PerfilEmpresa
import mz.co.kevin.concursos.data.model.PorteEmpresa
import mz.co.kevin.concursos.data.settings.AppSettings
import mz.co.kevin.concursos.data.settings.TemaApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes puros (JVM) que fixam os contratos dos modelos de domínio: rótulos das
 * enums, valores por omissão dos data classes e as listas de referência.
 */
class ModelosTest {

    @Test
    fun categoriaConcurso_rotulos() {
        assertEquals("Abertos", CategoriaConcurso.ABERTO.label)
        assertEquals("Adjudicados", CategoriaConcurso.ADJUDICADO.label)
        assertEquals("Cancelados", CategoriaConcurso.CANCELADO.label)
        assertEquals(3, CategoriaConcurso.entries.size)
    }

    @Test
    fun porteEmpresa_rotulos() {
        assertEquals("Micro ou Pequena Empresa", PorteEmpresa.MICRO_PEQUENA.label)
        assertEquals("Média Empresa", PorteEmpresa.MEDIA.label)
        assertEquals("Grande Empresa", PorteEmpresa.GRANDE.label)
    }

    @Test
    fun temaApp_rotulos() {
        assertEquals("Seguir o sistema", TemaApp.SISTEMA.label)
        assertEquals("Claro", TemaApp.CLARO.label)
        assertEquals("Escuro", TemaApp.ESCURO.label)
    }

    @Test
    fun appSettings_valores_por_omissao() {
        val s = AppSettings()
        assertEquals(TemaApp.SISTEMA, s.tema)
        assertTrue(s.coresDinamicas)
        assertTrue(s.notificacoesHabilitadas)
        assertFalse(s.notificarApenasTI)
        assertEquals(4, s.intervaloHoras)
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
