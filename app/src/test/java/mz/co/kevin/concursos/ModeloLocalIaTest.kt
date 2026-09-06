package mz.co.kevin.concursos

import mz.co.kevin.concursos.data.ai.ModeloLocalIa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contrato do registo de modelos locais abertos ([ModeloLocalIa]).
 */
class ModeloLocalIaTest {

    @Test
    fun padrao_e_o_qwen_1_5b() {
        assertEquals(ModeloLocalIa.QWEN_1_5B, ModeloLocalIa.PADRAO)
    }

    @Test
    fun porId_resolve_ou_cai_no_padrao() {
        assertEquals(ModeloLocalIa.TINYLLAMA_1_1B, ModeloLocalIa.porId("tinyllama_1_1b"))
        assertEquals(ModeloLocalIa.QWEN_1_5B, ModeloLocalIa.porId("qwen2_5_1_5b"))
        assertEquals(ModeloLocalIa.PADRAO, ModeloLocalIa.porId(null))
        assertEquals(ModeloLocalIa.PADRAO, ModeloLocalIa.porId("desconhecido"))
    }

    @Test
    fun cada_modelo_tem_metadados_validos() {
        val ids = ModeloLocalIa.entries.map { it.id }
        assertEquals("ids duplicados", ids.size, ids.toSet().size)
        ModeloLocalIa.entries.forEach { m ->
            assertTrue(m.id, m.url.startsWith("https://huggingface.co/"))
            assertTrue(m.id, m.url.endsWith(".task"))
            assertTrue(m.id, m.ficheiro.endsWith(".task"))
            assertTrue(m.id, m.nomeCurto.isNotBlank())
            assertTrue(m.id, m.tamanhoAprox.isNotBlank())
        }
    }
}
