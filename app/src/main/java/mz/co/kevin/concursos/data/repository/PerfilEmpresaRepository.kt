package mz.co.kevin.concursos.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mz.co.kevin.concursos.data.model.PerfilEmpresa
import mz.co.kevin.concursos.data.model.PorteEmpresa
import mz.co.kevin.concursos.data.model.RecomendacaoConcursoIa
import org.json.JSONArray
import org.json.JSONObject

class PerfilEmpresaRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("empresa_ai_prefs", Context.MODE_PRIVATE)

    private val _perfil = MutableStateFlow(lerPerfil())
    val perfil: StateFlow<PerfilEmpresa> = _perfil.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(lerApiKey())
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _recomendacoes = MutableStateFlow(lerRecomendacoes())
    val recomendacoes: StateFlow<List<RecomendacaoConcursoIa>> = _recomendacoes.asStateFlow()

    fun obterPerfilAtual(): PerfilEmpresa = _perfil.value

    fun obterApiKeyAtual(): String = _geminiApiKey.value

    fun salvarApiKey(key: String) {
        val limpa = key.trim()
        prefs.edit().putString(KEY_GEMINI_API_KEY, limpa).apply()
        _geminiApiKey.value = limpa
    }

    fun salvarPerfil(perfil: PerfilEmpresa) {
        val json = JSONObject().apply {
            put("nome", perfil.nome)
            put("nuit", perfil.nuit)
            put("porte", perfil.porte.name)
            put("especialidadesTexto", perfil.especialidadesTexto)
            put("outrosDocumentos", perfil.outrosDocumentos)
            put("configurado", perfil.configurado)

            val areasArr = JSONArray()
            perfil.areasAtuacao.forEach { areasArr.put(it) }
            put("areasAtuacao", areasArr)

            val provsArr = JSONArray()
            perfil.provinciasAtuacao.forEach { provsArr.put(it) }
            put("provinciasAtuacao", provsArr)

            val docsArr = JSONArray()
            perfil.documentosDisponiveis.forEach { docsArr.put(it) }
            put("documentosDisponiveis", docsArr)
        }

        prefs.edit().putString(KEY_PERFIL_JSON, json.toString()).apply()
        _perfil.value = perfil
    }

    fun salvarRecomendacoes(lista: List<RecomendacaoConcursoIa>) {
        val arr = JSONArray()
        for (item in lista) {
            val obj = JSONObject().apply {
                put("referencia", item.referencia)
                put("scoreCompatibilidade", item.scoreCompatibilidade)
                put("nivelCompatibilidade", item.nivelCompatibilidade)
                put("resumoAvaliacao", item.resumoAvaliacao)
                put("recomendacaoEstrategica", item.recomendacaoEstrategica)

                val pfArr = JSONArray()
                item.pontosFortes.forEach { pfArr.put(it) }
                put("pontosFortes", pfArr)

                val exArr = JSONArray()
                item.documentosExigidosProvaveis.forEach { exArr.put(it) }
                put("documentosExigidosProvaveis", exArr)

                val efArr = JSONArray()
                item.documentosEmFalta.forEach { efArr.put(it) }
                put("documentosEmFalta", efArr)
            }
            arr.put(obj)
        }

        prefs.edit().putString(KEY_RECOMENDACOES_JSON, arr.toString()).apply()
        _recomendacoes.value = lista
    }

    fun salvarAnaliseIndividual(item: RecomendacaoConcursoIa) {
        val cacheAtual = lerRecomendacoes().toMutableList()
        val index = cacheAtual.indexOfFirst { it.referencia == item.referencia }
        if (index >= 0) {
            cacheAtual[index] = item
        } else {
            cacheAtual.add(0, item)
        }
        salvarRecomendacoes(cacheAtual)
    }

    private fun lerApiKey(): String {
        return prefs.getString(KEY_GEMINI_API_KEY, "")?.trim().orEmpty()
    }

    private fun lerPerfil(): PerfilEmpresa {
        val raw = prefs.getString(KEY_PERFIL_JSON, null) ?: return PerfilEmpresa()
        return try {
            val json = JSONObject(raw)
            val porteStr = json.optString("porte", PorteEmpresa.MEDIA.name)
            val porte = runCatching { PorteEmpresa.valueOf(porteStr) }.getOrDefault(PorteEmpresa.MEDIA)

            val areas = mutableListOf<String>()
            json.optJSONArray("areasAtuacao")?.let { arr ->
                for (i in 0 until arr.length()) areas.add(arr.optString(i))
            }

            val provs = mutableListOf<String>()
            json.optJSONArray("provinciasAtuacao")?.let { arr ->
                for (i in 0 until arr.length()) provs.add(arr.optString(i))
            }

            val docs = mutableListOf<String>()
            json.optJSONArray("documentosDisponiveis")?.let { arr ->
                for (i in 0 until arr.length()) docs.add(arr.optString(i))
            }

            PerfilEmpresa(
                nome = json.optString("nome", ""),
                nuit = json.optString("nuit", ""),
                porte = porte,
                areasAtuacao = areas,
                especialidadesTexto = json.optString("especialidadesTexto", ""),
                provinciasAtuacao = if (provs.isEmpty()) listOf("Todas as Províncias") else provs,
                documentosDisponiveis = docs,
                outrosDocumentos = json.optString("outrosDocumentos", ""),
                configurado = json.optBoolean("configurado", false)
            )
        } catch (_: Exception) {
            PerfilEmpresa()
        }
    }

    private fun lerRecomendacoes(): List<RecomendacaoConcursoIa> {
        val raw = prefs.getString(KEY_RECOMENDACOES_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val lista = mutableListOf<RecomendacaoConcursoIa>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val pf = mutableListOf<String>()
                obj.optJSONArray("pontosFortes")?.let { a ->
                    for (j in 0 until a.length()) pf.add(a.optString(j))
                }

                val ex = mutableListOf<String>()
                obj.optJSONArray("documentosExigidosProvaveis")?.let { a ->
                    for (j in 0 until a.length()) ex.add(a.optString(j))
                }

                val ef = mutableListOf<String>()
                obj.optJSONArray("documentosEmFalta")?.let { a ->
                    for (j in 0 until a.length()) ef.add(a.optString(j))
                }

                lista.add(
                    RecomendacaoConcursoIa(
                        referencia = obj.optString("referencia", ""),
                        scoreCompatibilidade = obj.optInt("scoreCompatibilidade", 0),
                        nivelCompatibilidade = obj.optString("nivelCompatibilidade", "MEDIA"),
                        resumoAvaliacao = obj.optString("resumoAvaliacao", ""),
                        pontosFortes = pf,
                        documentosExigidosProvaveis = ex,
                        documentosEmFalta = ef,
                        recomendacaoEstrategica = obj.optString("recomendacaoEstrategica", "")
                    )
                )
            }
            lista
        } catch (_: Exception) {
            emptyList()
        }
    }

    private companion object {
        const val KEY_GEMINI_API_KEY = "gemini_api_key"
        const val KEY_PERFIL_JSON = "perfil_empresa_json"
        const val KEY_RECOMENDACOES_JSON = "recomendacoes_ia_json"
    }
}
