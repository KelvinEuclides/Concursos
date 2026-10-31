package mz.co.kevin.concursos.data.remote

import mz.co.kevin.concursos.data.model.CampoDetalhe
import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.DetalhesConcurso
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Scraper para o portal UNGM (United Nations Global Marketplace - ungm.org),
 * filtrando oportunidades de procurement focadas em Moçambique (Country ID 2432).
 */
object UngmScraper {
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    internal const val BASE_URL = "https://www.ungm.org/"
    internal const val NOTICE_URL = "https://www.ungm.org/Public/Notice"
    internal const val SEARCH_URL = "https://www.ungm.org/Public/Notice/Search"
    internal const val MOZAMBIQUE_COUNTRY_ID = "2432"

    private val cookieStore = ConcurrentHashMap<String, ConcurrentHashMap<String, Cookie>>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val hostCookies = cookieStore.computeIfAbsent(url.host) { ConcurrentHashMap() }
                for (cookie in cookies) {
                    hostCookies[cookie.name] = cookie
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host]?.values?.toList() ?: emptyList()
            }
        })
        .build()

    /**
     * Valida o HTML de resposta para detecção de bloqueios ou erros de servidor.
     */
    internal fun validarResposta(html: String) {
        if (html.contains("Can't connect to") || html.contains("Database error")) {
            throw IOException("O portal UNGM está temporariamente indisponível.")
        }
    }

    /**
     * Extrai o token de verificação anti-CSRF da página pública de editais do UNGM.
     */
    internal fun extrairToken(html: String): String {
        val doc = Jsoup.parse(html)
        return doc.selectFirst("input[name=__RequestVerificationToken]")?.attr("value").orEmpty()
    }

    /**
     * Converte o HTML retornado pela pesquisa do UNGM numa lista de [Concurso].
     * Função pura para testes unitários isolados.
     */
    internal fun parseConcursos(html: String, categoria: CategoriaConcurso): List<Concurso> {
        val doc = Jsoup.parse(html)
        val rows = doc.select("div.tableRow.dataRow.notice-table, div.dataRow")
        val lista = mutableListOf<Concurso>()

        for (row in rows) {
            val noticeId = row.attr("data-noticeid").trim()
            val cells = row.select("div.tableCell, div[role=cell]")

            val titleElem = row.selectFirst(".resultTitle .ungm-title--small, .resultTitle .ungm-title")
                ?: row.selectFirst(".resultTitle")
            val objecto = titleElem?.text()?.trim().orEmpty()
            if (objecto.isBlank() && noticeId.isBlank()) continue

            val linkAnchor = row.selectFirst("a[href*='/Public/Notice/']")?.attr("href").orEmpty()
            val linkCompleto = when {
                linkAnchor.isNotBlank() -> if (linkAnchor.startsWith("http")) linkAnchor else BASE_URL + linkAnchor.removePrefix("/")
                noticeId.isNotBlank() -> "${BASE_URL}Public/Notice/$noticeId"
                else -> ""
            }

            val rawDeadline = row.selectFirst(".tableCell.resultInfo1.deadline span, .tableCell.deadline span")?.text()?.trim()
                ?: row.selectFirst(".deadline")?.text()?.trim().orEmpty()
            val dataAbertura = rawDeadline.substringBefore("(").trim()
                .ifBlank { rawDeadline.lines().firstOrNull()?.trim() ?: rawDeadline }

            val dataLancamento = row.selectFirst(".resultAgency")?.previousElementSibling()?.selectFirst("span")?.text()?.trim()
                ?: row.selectFirst(".tableCell.deadline + .tableCell")?.text()?.trim()
                ?: cells.getOrNull(3)?.text()?.trim().orEmpty()

            val ugea = row.selectFirst(".resultAgency span")?.text()?.trim()
                ?: row.selectFirst(".resultAgency")?.text()?.trim()
                ?: cells.getOrNull(4)?.text()?.trim().orEmpty()

            val modalidade = row.selectFirst(".tableCell span label")?.text()?.trim()
                ?: row.selectFirst("label")?.text()?.trim()
                ?: cells.getOrNull(5)?.text()?.trim().orEmpty().ifBlank { "Oportunidade ONU" }

            val rawRef = row.selectFirst(".tableCell.resultInfo1[data-description=Reference] span")?.text()?.trim()
                ?: row.selectFirst("[data-description=Reference]")?.text()?.trim()
                ?: cells.getOrNull(6)?.text()?.trim().orEmpty()
            val referencia = when {
                rawRef.isNotBlank() && rawRef.startsWith("UNGM-") -> rawRef
                rawRef.isNotBlank() -> "UNGM-$rawRef"
                noticeId.isNotBlank() -> "UNGM-$noticeId"
                else -> "UNGM-" + Math.abs(objecto.hashCode())
            }

            val rawDest = row.selectFirst("[data-description=Reference] + .tableCell")?.text()?.trim()
                ?: cells.getOrNull(7)?.text()?.trim().orEmpty()
            val provincia = if (rawDest.isNotBlank() && !rawDest.contains("Multiple", ignoreCase = true)) {
                rawDest
            } else {
                "Moçambique"
            }

            lista.add(
                Concurso(
                    referencia = referencia,
                    modalidade = modalidade,
                    objecto = objecto,
                    ugea = ugea,
                    provincia = provincia,
                    dataLancamento = dataLancamento,
                    dataAbertura = dataAbertura,
                    linkDetalhes = linkCompleto,
                    categoria = categoria,
                    ehInformatica = UfsaScraper.ehInformatica(modalidade, objecto, ugea),
                    fonte = "UNGM"
                )
            )
        }
        return lista
    }

    /**
     * Converte o HTML de detalhes do edital UNGM num [DetalhesConcurso]. Função pura.
     */
    internal fun parseDetalhes(html: String, referencia: String): DetalhesConcurso {
        val doc = Jsoup.parse(html)
        val campos = mutableListOf<CampoDetalhe>()

        // Extrai pares de rótulo e valor (.row > .label e .value)
        val rowElements = doc.select(".row")
        for (r in rowElements) {
            val label = r.selectFirst(".label")?.text()?.removeSuffix(":")?.trim().orEmpty()
            val value = r.selectFirst(".value")?.text()?.trim().orEmpty()
            if (label.isNotBlank() && value.isNotBlank() && !label.equals(value, ignoreCase = true)) {
                campos.add(CampoDetalhe(label, value))
            }
        }

        // Descrição
        val descElem = doc.selectFirst("div.ungm-list-item:has(div.title:matchesOwn((?i)description)) > div:not(.title)")
            ?: doc.selectFirst("div.ungm-list-item .title:matchesOwn((?i)description) ~ div")
        val descTexto = descElem?.text()?.trim().orEmpty()
        if (descTexto.isNotBlank()) {
            campos.add(0, CampoDetalhe("Descrição", descTexto))
        }

        // Links de documentos
        val links = doc.select("#tblLinks tbody tr")
        var linkDoc = ""
        for (tr in links) {
            val linkUrl = tr.selectFirst("td")?.text()?.trim()
                ?: tr.selectFirst("a[href]")?.attr("href").orEmpty()
            if (linkUrl.startsWith("http")) {
                linkDoc = linkUrl
                break
            }
        }

        return DetalhesConcurso(
            referencia = referencia,
            campos = campos.distinct(),
            linkAnuncio = "",
            linkDocumento = linkDoc
        )
    }

    /**
     * Extrai concursos abertos ou fechados do UNGM para Moçambique.
     */
    fun extrairConcursos(categoria: CategoriaConcurso): List<Concurso> {
        // UNGM é primariamente portal de procurement de concursos abertos/activos.
        // Apenas consultamos em ABERTO para manter os concursos disponíveis na secção de Abertos.
        if (categoria != CategoriaConcurso.ABERTO) {
            return emptyList()
        }

        // 1. Obter página inicial para pegar cookies de sessão e token CSRF
        val getRequest = Request.Builder()
            .url(NOTICE_URL)
            .header("User-Agent", USER_AGENT)
            .build()

        val token: String
        client.newCall(getRequest).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Falha ao contactar UNGM: HTTP ${resp.code}")
            val initialHtml = resp.body?.string().orEmpty()
            validarResposta(initialHtml)
            token = extrairToken(initialHtml)
        }

        // 2. Realizar busca filtrando por Moçambique (CountryId 2432). O UNGM aceita apenas PageSize 15.
        val resultados = mutableListOf<Concurso>()
        for (pageIndex in 0..1) {
            val payload = """
                {
                  "PageIndex": $pageIndex,
                  "PageSize": 15,
                  "Title": "",
                  "Description": "",
                  "Reference": "",
                  "PublishedFrom": "",
                  "PublishedTo": "",
                  "DeadlineFrom": "",
                  "DeadlineTo": "",
                  "Countries": ["$MOZAMBIQUE_COUNTRY_ID"],
                  "Agencies": [],
                  "UNSPSCs": [],
                  "NoticeTypes": [],
                  "SortField": "Deadline",
                  "SortAscending": false,
                  "isPicker": false,
                  "IsSustainable": false,
                  "IsActive": true,
                  "NoticeDisplayType": "0",
                  "NoticeSearchTotalLabelId": "noticeSearchTotal",
                  "TypeOfCompetitions": []
                }
            """.trimIndent()

            val postRequest = Request.Builder()
                .url(SEARCH_URL)
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "text/html, */*; q=0.01")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("RequestVerificationToken", token)
                .header("Referer", NOTICE_URL)
                .post(payload.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                .build()

            client.newCall(postRequest).execute().use { resp ->
                if (!resp.isSuccessful) {
                    if (resultados.isNotEmpty()) return resultados
                    throw IOException("Erro na busca do UNGM: HTTP ${resp.code}")
                }
                val searchHtml = resp.body?.string().orEmpty()
                validarResposta(searchHtml)
                val items = parseConcursos(searchHtml, categoria)
                resultados.addAll(items)
                if (items.size < 15) {
                    return resultados
                }
            }
        }
        return resultados.distinctBy { it.referencia }
    }

    /**
     * Extrai os detalhes de um edital do UNGM.
     */
    fun extrairDetalhes(urlOuReferencia: String): DetalhesConcurso {
        val url = when {
            urlOuReferencia.startsWith("http", ignoreCase = true) -> urlOuReferencia
            urlOuReferencia.startsWith("UNGM-") -> "${BASE_URL}Public/Notice/${urlOuReferencia.removePrefix("UNGM-")}"
            else -> "${BASE_URL}Public/Notice/$urlOuReferencia"
        }

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()

        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} ao obter detalhes no UNGM")
            val html = resp.body?.string().orEmpty()
            validarResposta(html)
            return parseDetalhes(html, urlOuReferencia)
        }
    }
}
