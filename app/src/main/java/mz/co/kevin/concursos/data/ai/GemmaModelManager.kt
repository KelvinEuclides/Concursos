package mz.co.kevin.concursos.data.ai

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Gestor do ciclo de vida do ficheiro do modelo Gemma 2B no dispositivo.
 * Controla a verificação, download streaming com progresso, importação de ficheiro local
 * e remoção para libertação de espaço em disco.
 */
class GemmaModelManager(
    private val context: Context,
    private val client: OkHttpClient = defaultDownloadClient()
) {
    companion object {
        /** Slot único no disco — só um modelo local activo de cada vez. */
        const val MODEL_FILENAME = "modelo-ia-local.task"
        val DEFAULT_DOWNLOAD_URL: String get() = ModeloLocalIa.PADRAO.url

        /** Abaixo disto o ficheiro não é plausivelmente um modelo (é uma página de erro). */
        private const val TAMANHO_MINIMO_MODELO = 20L * 1024 * 1024

        private fun defaultDownloadClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        fun formatarTamanho(bytes: Long): String {
            if (bytes <= 0) return "0 MB"
            val mb = bytes.toDouble() / (1024 * 1024)
            return if (mb >= 1024) {
                val gb = mb / 1024
                String.format(Locale.US, "%.2f GB", gb)
            } else {
                String.format(Locale.US, "%.1f MB", mb)
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null

    private val modelsDirectory: File
        get() {
            val dir = context.getExternalFilesDir("models") ?: File(context.filesDir, "models")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    val modelFile: File
        get() = File(modelsDirectory, MODEL_FILENAME)

    private val tempDownloadFile: File
        get() = File(modelsDirectory, "$MODEL_FILENAME.download")

    private val _status = MutableStateFlow<GemmaStatus>(GemmaStatus.NaoInstalado)
    val status: StateFlow<GemmaStatus> = _status.asStateFlow()

    init {
        verificarStatus()
    }

    /**
     * Atualiza o estado atual do ficheiro no disco.
     */
    fun verificarStatus() {
        val file = modelFile
        _status.value = if (file.exists() && file.length() > 0) {
            GemmaStatus.Instalado(
                tamanhoBytes = file.length(),
                caminho = file.absolutePath
            )
        } else {
            GemmaStatus.NaoInstalado
        }
    }

    fun isModeloInstalado(): Boolean = modelFile.exists() && modelFile.length() > 0

    fun obterFicheiroModelo(): File? = if (isModeloInstalado()) modelFile else null

    /**
     * Verifica se [ficheiro] parece um modelo real e não uma página de erro
     * (HTML/JSON) gravada por engano. Devolve a mensagem de erro, ou null se OK.
     */
    private fun validarFicheiroModelo(ficheiro: File): String? {
        if (!ficheiro.exists() || ficheiro.length() < TAMANHO_MINIMO_MODELO) {
            return "O ficheiro é demasiado pequeno (${formatarTamanho(ficheiro.length())}) " +
                "para ser um modelo — a origem pode exigir início de sessão."
        }
        val inicio = ByteArray(64)
        val lidos = ficheiro.inputStream().use { it.read(inicio) }
        val texto = String(inicio, 0, lidos.coerceAtLeast(0)).trimStart().lowercase()
        if (texto.startsWith("<!doctype") || texto.startsWith("<html") || texto.startsWith("{\"error")) {
            return "O download devolveu uma página web em vez do modelo. Verifica o URL/origem."
        }
        return null
    }

    /**
     * Inicia o download do modelo via streaming HTTP com notificação de progresso.
     */
    fun iniciarDownload(url: String = DEFAULT_DOWNLOAD_URL): Job {
        val urlFinal = url.ifBlank { DEFAULT_DOWNLOAD_URL }
        downloadJob?.cancel()
        val job = scope.launch {
            try {
                _status.value = GemmaStatus.Descarregando(
                    progresso = 0f,
                    baixadoBytes = 0L,
                    totalBytes = 0L
                )

                if (tempDownloadFile.exists()) {
                    tempDownloadFile.delete()
                }

                val request = Request.Builder()
                    .url(urlFinal)
                    .header("User-Agent", "ConcursosAndroid/1.0")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("Falha no download: Código HTTP ${response.code} (${response.message})")
                }

                val body = response.body ?: throw IOException("Resposta do servidor vazia.")
                val totalBytes = body.contentLength()

                var baixados = 0L
                var ultimoTimestamp = 0L
                val buffer = ByteArray(32 * 1024)

                body.byteStream().use { input ->
                    tempDownloadFile.outputStream().use { output ->
                        while (isActive) {
                            val lidos = input.read(buffer)
                            if (lidos == -1) break
                            output.write(buffer, 0, lidos)
                            baixados += lidos

                            val agora = System.currentTimeMillis()
                            if (agora - ultimoTimestamp > 200 || baixados == totalBytes) {
                                ultimoTimestamp = agora
                                val progresso = if (totalBytes > 0) baixados.toFloat() / totalBytes else 0f
                                _status.value = GemmaStatus.Descarregando(
                                    progresso = progresso,
                                    baixadoBytes = baixados,
                                    totalBytes = totalBytes
                                )
                            }
                        }
                    }
                }

                if (!isActive) {
                    if (tempDownloadFile.exists()) tempDownloadFile.delete()
                    verificarStatus()
                    return@launch
                }

                if (tempDownloadFile.exists() && tempDownloadFile.length() > 0) {
                    validarFicheiroModelo(tempDownloadFile)?.let { erro ->
                        tempDownloadFile.delete()
                        throw IOException(erro)
                    }
                    if (modelFile.exists()) modelFile.delete()
                    val renomeado = tempDownloadFile.renameTo(modelFile)
                    if (renomeado) {
                        _status.value = GemmaStatus.Instalado(
                            tamanhoBytes = modelFile.length(),
                            caminho = modelFile.absolutePath
                        )
                    } else {
                        throw IOException("Não foi possível finalizar o ficheiro do modelo no disco.")
                    }
                } else {
                    throw IOException("Ficheiro descarregado vazio.")
                }
            } catch (e: Exception) {
                if (tempDownloadFile.exists()) runCatching { tempDownloadFile.delete() }
                if (isActive) {
                    _status.value = GemmaStatus.Erro(e.message ?: "Erro desconhecido durante o download")
                } else {
                    verificarStatus()
                }
            }
        }
        downloadJob = job
        return job
    }

    /**
     * Cancela qualquer download em curso e limpa o ficheiro temporário.
     */
    fun cancelarDownload() {
        downloadJob?.cancel()
        downloadJob = null
        if (tempDownloadFile.exists()) {
            runCatching { tempDownloadFile.delete() }
        }
        verificarStatus()
    }

    /**
     * Importa um ficheiro de modelo fornecido pelo utilizador (via Storage Access Framework).
     */
    suspend fun importarFicheiro(uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            _status.value = GemmaStatus.Carregando("A importar ficheiro do modelo...")
            if (tempDownloadFile.exists()) tempDownloadFile.delete()

            context.contentResolver.openInputStream(uri)?.use { input ->
                tempDownloadFile.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 32 * 1024)
                }
            } ?: return@withContext Result.failure(IOException("Não foi possível aceder ao ficheiro selecionado."))

            if (tempDownloadFile.exists() && tempDownloadFile.length() > 0) {
                validarFicheiroModelo(tempDownloadFile)?.let { erro ->
                    tempDownloadFile.delete()
                    verificarStatus()
                    return@withContext Result.failure(IOException(erro))
                }
                if (modelFile.exists()) modelFile.delete()
                val renomeado = tempDownloadFile.renameTo(modelFile)
                if (renomeado) {
                    val statusInstalado = GemmaStatus.Instalado(
                        tamanhoBytes = modelFile.length(),
                        caminho = modelFile.absolutePath
                    )
                    _status.value = statusInstalado
                    Result.success(modelFile)
                } else {
                    Result.failure(IOException("Falha ao mover ficheiro importado para a pasta de modelos."))
                }
            } else {
                Result.failure(IOException("O ficheiro importado está vazio."))
            }
        } catch (e: Exception) {
            verificarStatus()
            Result.failure(e)
        }
    }

    /**
     * Remove o ficheiro do modelo para libertar armazenamento.
     */
    fun eliminarModelo(): Boolean {
        cancelarDownload()
        var sucesso = true
        if (modelFile.exists()) {
            sucesso = modelFile.delete()
        }
        if (tempDownloadFile.exists()) {
            tempDownloadFile.delete()
        }
        verificarStatus()
        return sucesso
    }
}
