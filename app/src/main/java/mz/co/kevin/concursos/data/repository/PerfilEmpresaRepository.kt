package mz.co.kevin.concursos.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mz.co.kevin.concursos.data.model.ChecklistProposta
import mz.co.kevin.concursos.data.model.ItemChecklist
import mz.co.kevin.concursos.data.model.PerfilEmpresa
import mz.co.kevin.concursos.data.model.PorteEmpresa
import mz.co.kevin.concursos.data.model.RecomendacaoConcursoIa
import mz.co.kevin.concursos.data.model.SeccaoProposta
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

    // --- Checklist de proposta (feature #47) — mapa { referencia -> checklist } ---

    fun obterChecklist(referencia: String): ChecklistProposta? {
        val raw = prefs.getString(KEY_CHECKLISTS_JSON, null) ?: return null
        return runCatching {
            val mapa = JSONObject(raw)
            mapa.optJSONObject(referencia)?.let { checklistDeJson(it, referencia) }
        }.getOrNull()
    }

    fun salvarChecklist(checklist: ChecklistProposta) {
        val raw = prefs.getString(KEY_CHECKLISTS_JSON, null)
        val mapa = runCatching { if (raw != null) JSONObject(raw) else JSONObject() }.getOrDefault(JSONObject())
        mapa.put(checklist.referencia, checklistParaJson(checklist))
        prefs.edit().putString(KEY_CHECKLISTS_JSON, mapa.toString()).apply()
    }

    private fun checklistParaJson(c: ChecklistProposta): JSONObject = JSONObject().apply {
        put("referencia", c.referencia)
        put("formatoEntrega", c.formatoEntrega)
        put("datasChave", JSONArray().apply { c.datasChave.forEach { put(it) } })
        put("documentos", JSONArray().apply {
            c.documentos.forEach { item ->
                put(JSONObject().apply {
                    put("texto", item.texto)
                    put("disponivel", item.disponivel)
                    put("concluido", item.concluido)
                })
            }
        })
        put("esqueleto", JSONArray().apply {
            c.esqueleto.forEach { sec ->
                put(JSONObject().apply {
                    put("titulo", sec.titulo)
                    put("pontos", JSONArray().apply { sec.pontos.forEach { put(it) } })
                })
            }
        })
    }

    private fun checklistDeJson(o: JSONObject, referencia: String): ChecklistProposta {
        fun arr(name: String): List<String> = o.optJSONArray(name)?.let { a ->
            (0 until a.length()).map { a.optString(it) }
        } ?: emptyList()

        val docs = o.optJSONArray("documentos")?.let { a ->
            (0 until a.length()).map { i ->
                val d = a.getJSONObject(i)
                ItemChecklist(
                    texto = d.optString("texto"),
                    disponivel = d.optBoolean("disponivel", false),
                    concluido = d.optBoolean("concluido", false),
                )
            }
        } ?: emptyList()

        val esqueleto = o.optJSONArray("esqueleto")?.let { a ->
            (0 until a.length()).map { i ->
                val s = a.getJSONObject(i)
                val pontos = s.optJSONArray("pontos")?.let { p ->
                    (0 until p.length()).map { p.optString(it) }
                } ?: emptyList()
                SeccaoProposta(titulo = s.optString("titulo"), pontos = pontos)
            }
        } ?: emptyList()

        return ChecklistProposta(
            referencia = referencia,
            documentos = docs,
            formatoEntrega = o.optString("formatoEntrega"),
            datasChave = arr("datasChave"),
            esqueleto = esqueleto,
        )
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
        const val KEY_CHECKLISTS_JSON = "checklists_proposta_json"
    }
}
