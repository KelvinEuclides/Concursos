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
import mz.co.kevin.concursos.MainActivity
import mz.co.kevin.concursos.UfsaApplication
import mz.co.kevin.concursos.R
import mz.co.kevin.concursos.data.model.Concurso

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
                dispararNotificacao(novos)
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

    private fun dispararNotificacao(novos: List<Concurso>) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

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
        val corpo = buildString {
            append(linhas.joinToString("\n"))
            if (novos.size > 8) append(context.getString(R.string.notif_mais, novos.size - 8))
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
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            // Faz o launcher mostrar o número de novos concursos no emblema do ícone.
            .setNumber(novos.size)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .build()

        // notify() é seguro sem POST_NOTIFICATIONS; o sistema simplesmente ignora se negado.
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            nm.notify(NOTIF_ID, notif)
        }
    }

    private companion object {
        const val CANAL_ID = "ufsa_concursos_channel"
        const val NOTIF_ID = 1001
    }
}
