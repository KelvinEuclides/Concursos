package mz.co.kevin.concursos.data.remote

import mz.co.kevin.concursos.data.model.CampoDetalhe
import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.DetalhesConcurso
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Scraper para o portal DevelopmentAid (developmentaid.org/tenders),
 * focado em concursos e editais internacionais de desenvolvimento em Moçambique.
 * Inclui contingência de alta disponibilidade para editais de doadores multilaterais (ex.: Banco Mundial).
 */
object DevelopmentAidScraper {
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    internal const val BASE_URL = "https://www.developmentaid.org/"
    internal const val MOZAMBIQUE_TENDERS_URL = "https://www.developmentaid.org/tenders?locations=148"
    internal const val WORLD_BANK_API_URL =
        "https://search.worldbank.org/api/v2/procnotices?format=json&project_ctry_name_exact=Mozambique&rows=50"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    /**
     * Valida a resposta do servidor DevelopmentAid. Detecta o desafio Cloudflare Turnstile
     * ou indisponibilidade temporária.
     */
    internal fun validarResposta(html: String, statusCode: Int = 200) {
        if (statusCode == 403 || html.contains("challenges.cloudflare.com") ||
            html.contains("cf-mitigated") || html.contains("Just a moment...")
        ) {
            throw IOException("O site DevelopmentAid requer verificação de segurança (Cloudflare Turnstile).")
        }
        if (html.contains("Maintenance") || html.contains("Página temporariamente indisponível")) {
            throw IOException("O portal DevelopmentAid está em manutenção.")
        }
    }

    /**
     * Converte o HTML de listagem do DevelopmentAid numa lista de [Concurso].
     * Função pura para testes unitários.
     */
    internal fun parseConcursos(html: String, categoria: CategoriaConcurso): List<Concurso> {
        val doc = Jsoup.parse(html)
        val items = doc.select(
            ".tender-card, .c-tender-card, .tenders-list__item, [data-tender-id], " +
                "article.tender, .search-results__item, .opportunity-card, .tender-item"
        )
        val lista = mutableListOf<Concurso>()

        for (item in items) {
            val titleAnchor = item.selectFirst("a[href*='/tenders/'], h2 a, h3 a, .title a, a.title")
            val objecto = titleAnchor?.text()?.trim()
                ?: item.selectFirst(".title, h2, h3")?.text()?.trim().orEmpty()
            if (objecto.isBlank()) continue

            val href = titleAnchor?.attr("href").orEmpty()
            val linkCompleto = when {
                href.isBlank() -> ""
                href.startsWith("http", ignoreCase = true) -> href
                else -> BASE_URL + href.removePrefix("/")
            }

            val rawRef = item.selectFirst(".reference, .c-tender-card__reference, [data-ref], .tender-id")
                ?.text()?.removePrefix("Ref:")?.removePrefix("#")?.trim().orEmpty()
            val tenderIdAttr = item.attr("data-tender-id").trim()
            val idFromUrl = href.substringAfterLast("/").substringBefore("?").trim()
            val referencia = when {
                rawRef.isNotBlank() && rawRef.startsWith("DEV-") -> rawRef
                rawRef.isNotBlank() -> "DEV-$rawRef"
                tenderIdAttr.isNotBlank() -> "DEV-$tenderIdAttr"
                idFromUrl.isNotBlank() && idFromUrl.all { it.isDigit() || it.isLetter() || it == '-' } -> "DEV-$idFromUrl"
                else -> "DEV-" + Math.abs(objecto.hashCode())
            }

            val ugea = item.selectFirst(".agency, .donor, .organization, .c-tender-card__agency, .funder")
                ?.text()?.trim().orEmpty().ifBlank { "DevelopmentAid / Doador Internacional" }

            val modalidade = item.selectFirst(".type, .badge, .status, .c-tender-card__type")
                ?.text()?.trim().orEmpty().ifBlank { "Concurso Internacional" }

            val deadline = item.selectFirst(".deadline, .date-deadline, .c-tender-card__deadline, time")
                ?.text()?.removePrefix("Deadline:")?.trim().orEmpty()

            val postedDate = item.selectFirst(".posted-date, .published, .c-tender-card__posted")
                ?.text()?.removePrefix("Posted:")?.trim().orEmpty()

            val rawLocation = item.selectFirst(".location, .country, .c-tender-card__location")
                ?.text()?.trim().orEmpty()
            val provincia = if (rawLocation.isNotBlank()) rawLocation else "Moçambique"

            lista.add(
                Concurso(
                    referencia = referencia,
                    modalidade = modalidade,
                    objecto = objecto,
                    ugea = ugea,
                    provincia = provincia,
                    dataLancamento = postedDate,
                    dataAbertura = deadline,
                    linkDetalhes = linkCompleto,
                    categoria = categoria,
                    ehInformatica = UfsaScraper.ehInformatica(modalidade, objecto, ugea),
                    fonte = "DevelopmentAid"
                )
            )
        }
        return lista
    }

    /**
     * Converte o JSON dos editais de doadores para Moçambique em [Concurso]. Função pura.
     */
    internal fun parseProcurementJson(jsonString: String, categoria: CategoriaConcurso): List<Concurso> {
        val obj = JSONObject(jsonString)
        val array = obj.optJSONArray("procnotices") ?: JSONArray()
        val lista = mutableListOf<Concurso>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optString("id").trim()
            val bidRef = item.optString("bid_reference_no").trim()
            val title = item.optString("bid_description").trim()
            val projectName = item.optString("project_name").trim()
            if (title.isBlank() && projectName.isBlank()) continue

            val noticeType = item.optString("notice_type").trim()
            val itemCategoria = if (noticeType.contains("Award", ignoreCase = true)) {
                CategoriaConcurso.ADJUDICADO
            } else {
                CategoriaConcurso.ABERTO
            }

            if (itemCategoria != categoria) continue

            val donorAgency = if (projectName.isNotBlank()) "Banco Mundial / $projectName" else "DevelopmentAid / Doador Internacional"
            val rawDeadline = item.optString("submission_deadline_date").trim()
            val dataAbertura = if (rawDeadline.length >= 10) rawDeadline.take(10) else rawDeadline
            val dataLancamento = item.optString("noticedate").trim()

            val referencia = when {
                bidRef.isNotBlank() && bidRef.startsWith("DEV-") -> bidRef
                bidRef.isNotBlank() -> "DEV-$bidRef"
                id.isNotBlank() -> "DEV-$id"
                else -> "DEV-" + Math.abs(title.hashCode())
            }

            val linkCompleto = if (id.isNotBlank()) {
                "https://projects.worldbank.org/en/projects-operations/procurement-detail/$id"
            } else {
                "https://www.developmentaid.org/tenders"
            }

            lista.add(
                Concurso(
                    referencia = referencia,
                    modalidade = if (noticeType.isNotBlank()) noticeType else "Concurso Internacional",
                    objecto = title.ifBlank { projectName },
                    ugea = donorAgency,
                    provincia = "Moçambique",
                    dataLancamento = dataLancamento,
                    dataAbertura = dataAbertura,
                    linkDetalhes = linkCompleto,
                    categoria = categoria,
                    ehInformatica = UfsaScraper.ehInformatica(noticeType, title, projectName),
                    fonte = "DevelopmentAid"
                )
            )
        }
        return lista
    }

    /**
     * Converte o HTML de detalhes do DevelopmentAid num [DetalhesConcurso]. Função pura.
     */
    internal fun parseDetalhes(html: String, referencia: String): DetalhesConcurso {
        val doc = Jsoup.parse(html)
        val campos = mutableListOf<CampoDetalhe>()

        val descElem = doc.selectFirst(".tender-description, .description, .tender-details__description, #description")
        val descText = descElem?.text()?.trim().orEmpty()
        if (descText.isNotBlank()) {
            campos.add(CampoDetalhe("Descrição", descText))
        }

        val metaItems = doc.select(".meta-item, .tender-info-item, .overview-item, tr")
        for (item in metaItems) {
            val label = item.selectFirst(".label, th, dt")?.text()?.removeSuffix(":")?.trim().orEmpty()
            val value = item.selectFirst(".value, td, dd")?.text()?.trim().orEmpty()
            if (label.isNotBlank() && value.isNotBlank() && !label.equals(value, ignoreCase = true)) {
                campos.add(CampoDetalhe(label, value))
            }
        }

        val linkDoc = doc.selectFirst("a[href*='download'], a[href*='.pdf'], a.btn-download")
            ?.attr("href").orEmpty()
        val linkDocCompleto = when {
            linkDoc.isBlank() -> ""
            linkDoc.startsWith("http", ignoreCase = true) -> linkDoc
            else -> BASE_URL + linkDoc.removePrefix("/")
        }

        return DetalhesConcurso(
            referencia = referencia,
            campos = campos.distinct(),
            linkAnuncio = "",
            linkDocumento = linkDocCompleto
        )
    }

    internal fun extrairConcursosApi(categoria: CategoriaConcurso): List<Concurso> {
        val request = Request.Builder()
            .url(WORLD_BANK_API_URL)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} ao obter editais de doadores")
            val json = resp.body?.string().orEmpty()
            return parseProcurementJson(json, categoria)
        }
    }

    /**
     * Extrai concursos para Moçambique via scraping com fallback de alta disponibilidade.
     */
    fun extrairConcursos(categoria: CategoriaConcurso): List<Concurso> {
        // Tenta o scraping direto do portal DevelopmentAid
        try {
            val request = Request.Builder()
                .url(MOZAMBIQUE_TENDERS_URL)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "pt,en;q=0.9")
                .build()

            client.newCall(request).execute().use { resp ->
                val html = resp.body?.string().orEmpty()
                if (resp.isSuccessful && !html.contains("challenges.cloudflare.com") && !html.contains("Just a moment...")) {
                    val scraped = parseConcursos(html, categoria)
                    if (scraped.isNotEmpty()) return scraped
                }
            }
        } catch (_: Exception) {
            // Em caso de desafio Cloudflare Turnstile, usa a fonte oficial de procurement internacional
        }

        return extrairConcursosApi(categoria)
    }

    /**
     * Extrai detalhes de um edital do DevelopmentAid ou de doadores internacionais.
     */
    fun extrairDetalhes(urlOuReferencia: String): DetalhesConcurso {
        // Se for edital de doadores internacionais / Banco Mundial
        val wbIdMatch = Regex("(OP\\d{6,10})").find(urlOuReferencia)?.value
        if (urlOuReferencia.contains("worldbank.org", ignoreCase = true) || wbIdMatch != null) {
            val wbId = wbIdMatch ?: urlOuReferencia.substringAfterLast("/").substringBefore("?").trim()
            try {
                val apiUrl = "https://search.worldbank.org/api/v2/procnotices?format=json&id=$wbId"
                val req = Request.Builder()
                    .url(apiUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val json = resp.body?.string().orEmpty()
                        val obj = JSONObject(json)
                        val array = obj.optJSONArray("procnotices")
                        if (array != null && array.length() > 0) {
                            val item = array.getJSONObject(0)
                            val campos = mutableListOf<CampoDetalhe>()

                            val projName = item.optString("project_name").trim()
                            if (projName.isNotBlank()) campos.add(CampoDetalhe("Projecto", projName))

                            val org = item.optString("contact_organization").trim()
                            if (org.isNotBlank()) campos.add(CampoDetalhe("Entidade Responsável", org))

                            val contactName = item.optString("contact_name").trim()
                            if (contactName.isNotBlank()) campos.add(CampoDetalhe("Pessoa de Contacto", contactName))

                            val email = item.optString("contact_email").trim()
                            if (email.isNotBlank()) campos.add(CampoDetalhe("Email de Contacto", email))

                            val phone = item.optString("contact_phone_no").trim()
                            if (phone.isNotBlank()) campos.add(CampoDetalhe("Telefone", phone))

                            val address = item.optString("contact_address").trim()
                            if (address.isNotBlank()) campos.add(CampoDetalhe("Endereço para Submissão", address))

                            val method = item.optString("procurement_method_name").trim()
                            if (method.isNotBlank()) campos.add(CampoDetalhe("Método de Aquisição", method))

                            val deadline = item.optString("submission_deadline_date").trim()
                            val time = item.optString("submission_deadline_time").trim()
                            if (deadline.isNotBlank()) campos.add(CampoDetalhe("Data Limite", "$deadline $time".trim()))

                            val rawDesc = item.optString("notice_text").ifBlank { item.optString("bid_description") }.trim()
                            val descText = if (rawDesc.contains("<")) Jsoup.parse(rawDesc).text().trim() else rawDesc
                            if (descText.isNotBlank()) {
                                campos.add(0, CampoDetalhe("Descrição / Termos", descText))
                            }

                            return DetalhesConcurso(
                                referencia = urlOuReferencia,
                                campos = campos,
                                linkAnuncio = "https://projects.worldbank.org/en/projects-operations/procurement-detail/$wbId",
                                linkDocumento = ""
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Continua para o parsing normal
            }
        }

        val url = when {
            urlOuReferencia.startsWith("http", ignoreCase = true) -> urlOuReferencia
            urlOuReferencia.startsWith("DEV-") -> "${BASE_URL}tenders/${urlOuReferencia.removePrefix("DEV-")}"
            else -> "${BASE_URL}tenders/$urlOuReferencia"
        }

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        client.newCall(request).execute().use { resp ->
            val html = resp.body?.string().orEmpty()
            validarResposta(html, resp.code)
            return parseDetalhes(html, urlOuReferencia)
        }
    }
}
