package mz.co.kevin.concursos.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.withTimeoutOrNull
import mz.co.kevin.concursos.MainActivity
import mz.co.kevin.concursos.UfsaApplication
import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.RecomendacaoConcursoIa

class DailySyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = UfsaApplication.settings.atual()
        val repo = UfsaApplication.repository
        return try {
            // Em segundo plano NÃO marca como visto — só os concursos realmente
            // novos (nunca vistos) chegam aqui.
            val novos = repo.sincronizarConcursos(marcarComoVistos = false)

            if (settings.notificacoesHabilitadas && novos.isNotEmpty()) {
                // Feature #46: se o utilizador optou por "só concursos relevantes",
                // corre a triagem por IA e notifica apenas os de alta compatibilidade.
                // Qualquer falha/timeout cai no comportamento normal (todos os novos).
                val relevantes = if (settings.notificarApenasRelevantesIa) {
                    triarPorIa(novos)
                } else {
                    null
                }

                when {
                    relevantes == null -> dispararNotificacao(novos)
                    relevantes.isNotEmpty() -> dispararNotificacaoAlta(relevantes)
                    // IA disponível mas nada de alta compatibilidade: não notifica.
                    else -> Unit
                }
            }
            // Regista todos os novos como vistos: já foram considerados neste ciclo,
            // não devem voltar a notificar mesmo que o utilizador não abra a app.
            if (novos.isNotEmpty()) {
                repo.marcarConcursosVistos(novos.map { it.referencia })
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    /**
     * Corre a triagem por IA sobre os [novos] concursos e devolve os pares
     * `(concurso, recomendação)` de alta compatibilidade. Devolve `null` quando
     * a triagem não pôde correr (sem perfil, sem IA, timeout ou erro) — o
     * chamador deve então cair no comportamento normal.
     */
    private suspend fun triarPorIa(novos: List<Concurso>): List<Pair<Concurso, RecomendacaoConcursoIa>>? {
        val perfilRepo = UfsaApplication.perfilRepository
        val perfil = perfilRepo.obterPerfilAtual()
        if (!perfil.configurado) return null

        val apiKey = perfilRepo.obterApiKeyAtual()
        val recomendacoes = withTimeoutOrNull(TIMEOUT_TRIAGEM_MS) {
            UfsaApplication.googleAiService
                .selecionarConcursos(apiKey, perfil, novos)
                .getOrNull()
        } ?: return null

        // Guarda para o ecrã já mostrar as recomendações quando o utilizador abre.
        if (recomendacoes.isNotEmpty()) {
            runCatching { perfilRepo.salvarRecomendacoes(recomendacoes) }
        }

        return filtrarAltaCompatibilidade(novos, recomendacoes)
    }

    private fun dispararNotificacaoAlta(relevantes: List<Pair<Concurso, RecomendacaoConcursoIa>>) {
        garantirCanal()

        val titulo = if (relevantes.size == 1) {
            context.getString(R.string.notif_titulo_alta_um)
        } else {
            context.getString(R.string.notif_titulo_alta_varios, relevantes.size)
        }

        val linhas = relevantes.take(8).map { (c, rec) ->
            val score = context.getString(R.string.notif_item_score, rec.scoreCompatibilidade)
            "• ${c.objecto.take(90)}$score"
        }
        val extra = relevantes.size - linhas.size

        val estilo = NotificationCompat.InboxStyle().setBigContentTitle(titulo)
        linhas.forEach { estilo.addLine(it) }
        if (extra > 0) estilo.setSummaryText(context.getString(R.string.notif_resumo_mais, extra))

        val notif = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(titulo)
            .setContentText(relevantes.first().first.objecto.take(90))
            .setStyle(estilo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent())
            .setAutoCancel(true)
            .setNumber(relevantes.size)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .build()

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            notificationManager().notify(NOTIF_ID, notif)
        }
    }

    private fun dispararNotificacao(novos: List<Concurso>) {
        garantirCanal()

        val titulo = when {
            novos.size == 1 -> context.getString(R.string.notif_titulo_um)
            else -> context.getString(R.string.notif_titulo_varios, novos.size)
        }

        // Detalhes: uma linha por concurso com objecto, UGEA e data de abertura.
        val linhas = novos.take(8).map { c ->
            val abertura = if (c.dataAbertura.isNotBlank()) context.getString(R.string.notif_item_abre, c.dataAbertura) else ""
            val ugea = if (c.ugea.isNotBlank()) context.getString(R.string.notif_item_ugea, c.ugea) else ""
            "• ${c.objecto.take(90)}$ugea$abertura"
        }

        val estilo = NotificationCompat.InboxStyle().setBigContentTitle(titulo)
        linhas.forEach { estilo.addLine(it) }
        if (novos.size > 8) estilo.setSummaryText(context.getString(R.string.notif_resumo_mais, novos.size - 8))

        val notif = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(titulo)
            .setContentText(novos.firstOrNull()?.objecto?.take(90) ?: context.getString(R.string.notif_toque_ver))
            .setStyle(estilo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent())
            .setAutoCancel(true)
            // Faz o launcher mostrar o número de novos concursos no emblema do ícone.
            .setNumber(novos.size)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .build()

        // notify() é seguro sem POST_NOTIFICATIONS; o sistema simplesmente ignora se negado.
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            notificationManager().notify(NOTIF_ID, notif)
        }
    }

    private fun notificationManager() =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun garantirCanal() {
        val nm = notificationManager()
        if (nm.getNotificationChannel(CANAL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CANAL_ID,
                    context.getString(R.string.notif_canal_nome),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notif_canal_desc)
                }
            )
        }
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        private const val CANAL_ID = "ufsa_concursos_channel"
        private const val NOTIF_ID = 1001
        private const val TIMEOUT_TRIAGEM_MS = 90_000L

        /** Score mínimo para uma recomendação contar como "alta compatibilidade". */
        const val SCORE_ALTA = 70

        /**
         * Cruza os [novos] concursos com as [recomendacoes] da IA e devolve, pela
         * ordem dos concursos novos, os pares de alta compatibilidade
         * (nível "ALTA" ou score ≥ [SCORE_ALTA]). Puro — testável em JVM.
         */
        fun filtrarAltaCompatibilidade(
            novos: List<Concurso>,
            recomendacoes: List<RecomendacaoConcursoIa>
        ): List<Pair<Concurso, RecomendacaoConcursoIa>> {
            val porRef = recomendacoes.associateBy { it.referencia }
            return novos.mapNotNull { c ->
                val rec = porRef[c.referencia] ?: return@mapNotNull null
                val alta = rec.nivelCompatibilidade.equals("ALTA", ignoreCase = true) ||
                    rec.scoreCompatibilidade >= SCORE_ALTA
                if (alta) c to rec else null
            }
        }
    }
}
