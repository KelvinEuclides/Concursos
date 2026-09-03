package mz.co.kevin.concursos.data.remote

import mz.co.kevin.concursos.data.model.CampoDetalhe
import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.DetalhesConcurso
import mz.co.kevin.concursos.data.model.FornecedorCef
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import java.util.concurrent.TimeUnit

object UfsaScraper {
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    private const val BASE_URL = "https://www.ufsa.gov.mz/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    internal val PALAVRAS_CHAVE_TI = listOf(
        "informa", "comput", "software", "sistema", "rede",
        "servidor", "tecnolog", "fibra", "hardware", "antivirus",
        "dados", "datacenter", "web", "impressora", "ti ", "tic"
    )

    internal fun endpointDe(categoria: CategoriaConcurso): String = when (categoria) {
        CategoriaConcurso.ABERTO -> BASE_URL + "query/Busca_concurso1.php?dado="
        CategoriaConcurso.ADJUDICADO -> BASE_URL + "query/Busca_adjudicacao.php?dado="
        CategoriaConcurso.CANCELADO -> BASE_URL + "query/Busca_cancelamento.php?dado="
    }

    /**
     * Marca um concurso como sendo da área de TI quando qualquer palavra do
     * cluster informático aparece na modalidade, objecto ou UGEA.
     */
    internal fun ehInformatica(vararg campos: String): Boolean {
        val texto = campos.joinToString(" ").lowercase()
        return PALAVRAS_CHAVE_TI.any { kw -> texto.contains(kw) }
    }

    /**
     * Detecta as telas de erro do servidor legado da UFSA. Quando o backend cai
     * (socket MySQL) ou o proxy responde "Gateway inválido", abortamos com
     * [IOException] para que o Room mantenha os dados já persistidos.
     */
    internal fun validarResposta(html: String) {
        if (html.contains("Can't connect to local MySQL") || html.contains("mysqld.sock")) {
            throw IOException("O banco de dados da UFSA está temporariamente indisponível.")
        }
        if (html.contains("Gateway inválido") || html.contains("Página temporariamente indisponível")) {
            throw IOException("O servidor proxy da UFSA retornou Gateway Inválido.")
        }
    }

    /**
     * Converte o HTML da tabela `#lista` numa lista de [Concurso]. Função pura:
     * não toca na rede nem no armazenamento, para poder ser testada isoladamente.
     */
    internal fun parseConcursos(html: String, categoria: CategoriaConcurso): List<Concurso> {
        val doc = Jsoup.parse(html)
        val linhas = doc.select("table#lista tr[align=left]")
        val lista = mutableListOf<Concurso>()

        for (tr in linhas) {
            val colunas = tr.select("td")
            if (colunas.size < 3) continue

            val td0 = colunas[0]
            val linkHref = td0.selectFirst("a[href*=referencia=]")?.attr("href").orEmpty()
            val linkCompleto = if (linkHref.isNotBlank()) BASE_URL + linkHref else ""

            val ref = if (linkHref.contains("referencia=")) {
                linkHref.substringAfter("referencia=").substringBefore("&").trim()
            } else {
                td0.text().substringAfter(":").trim()
            }
            if (ref.isBlank()) continue

            val modalidade = td0.text().substringBefore(":").trim()
            val objecto = colunas.getOrNull(1)?.text()?.trim().orEmpty()
            val ugea = colunas.getOrNull(2)?.text()?.trim().orEmpty()
            val provincia = colunas.getOrNull(3)?.text()?.trim().orEmpty()
            val dtLanc = colunas.getOrNull(4)?.text()?.trim().orEmpty()
            val dtAbert = colunas.getOrNull(5)?.text()?.trim().orEmpty()

            lista.add(
                Concurso(
                    referencia = ref,
                    modalidade = modalidade,
                    objecto = objecto,
                    ugea = ugea,
                    provincia = provincia,
                    dataLancamento = dtLanc,
                    dataAbertura = dtAbert,
                    linkDetalhes = linkCompleto,
                    categoria = categoria,
                    ehInformatica = ehInformatica(modalidade, objecto, ugea)
                )
            )
        }
        return lista
    }

    /**
     * Converte o HTML da tabela `#lista` numa lista de [FornecedorCef]. Função pura.
     */
    internal fun parseFornecedores(html: String): List<FornecedorCef> {
        val doc = Jsoup.parse(html)
        val linhas = doc.select("table#lista tr[align=left]")
        val lista = mutableListOf<FornecedorCef>()

        for (tr in linhas) {
            val tds = tr.select("td")
            if (tds.size < 6) continue

            val cert = tds[0].text().trim()
            if (cert.isBlank()) continue

            lista.add(
                FornecedorCef(
                    certificado = cert,
                    nome = tds[1].text().trim(),
                    nuit = tds[2].text().trim(),
                    provincia = tds[3].text().trim(),
                    dataInscricao = tds[4].text().trim(),
                    actividades = tds[5].text().trim()
                )
            )
        }
        return lista
    }

    /**
     * Converte o HTML de `concurso_detalhes.php` num [DetalhesConcurso].
     * A página usa uma `table#lista` com linhas `<th>Rótulo:</th><td>valor</td>`
     * (as primeiras linhas usam `<th>` também para o valor) e uma `table#lista2`
     * com os links de descarga. Função pura.
     */
    internal fun parseDetalhes(html: String, referencia: String): DetalhesConcurso {
        val doc = Jsoup.parse(html)

        val campos = doc.select("table#lista tr").mapNotNull { tr ->
            val celulas = tr.select("th, td")
                .map { it.wholeText().trim().replace(Regex("\\s+\n"), "\n") }
                .filter { it.isNotEmpty() }
            if (celulas.size < 2) return@mapNotNull null
            val rotulo = celulas[0].removeSuffix(":").trim()
            val valor = celulas.drop(1).joinToString(" ").trim()
            if (rotulo.isEmpty() || valor.isEmpty()) null else CampoDetalhe(rotulo, valor)
        }

        val links = doc.select("table#lista2 a[href]")
        val anuncio = links.firstOrNull { it.attr("href").contains("Baixar_anuncio", true) }
            ?.attr("href").orEmpty()
        val documento = links.firstOrNull { it.attr("href").contains("Baixar_cad_enc", true) }
            ?.attr("href").orEmpty()

        return DetalhesConcurso(
            referencia = referencia,
            campos = campos,
            linkAnuncio = urlAbsoluta(anuncio),
            linkDocumento = urlAbsoluta(documento)
        )
    }

    internal fun urlAbsoluta(href: String): String = when {
        href.isBlank() -> ""
        href.startsWith("http", ignoreCase = true) -> href
        else -> BASE_URL + href.removePrefix("/")
    }

    private fun baixarHtml(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val html = response.body?.string().orEmpty()
            validarResposta(html)
            return html
        }
    }

    fun extrairConcursos(categoria: CategoriaConcurso): List<Concurso> =
        parseConcursos(baixarHtml(endpointDe(categoria)), categoria)

    fun buscarFornecedoresCEF(provincia: String, termo: String): List<FornecedorCef> {
        val url = BASE_URL +
            "query/Busca_inscritoscef.php?provincia=${provincia.trim()}&dado=${termo.trim()}"
        return parseFornecedores(baixarHtml(url))
    }

    fun extrairDetalhes(referencia: String): DetalhesConcurso {
        val url = BASE_URL + "concurso_detalhes.php?referencia=${referencia.trim()}"
        return parseDetalhes(baixarHtml(url), referencia.trim())
    }
}
