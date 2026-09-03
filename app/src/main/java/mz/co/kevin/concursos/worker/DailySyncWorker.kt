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
                    "Novos concursos - UFSA",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alertas sobre novos concursos publicados no portal da UFSA"
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
            novos.size == 1 -> "Novo concurso na UFSA"
            else -> "${novos.size} novos concursos na UFSA"
        }

        // Detalhes: uma linha por concurso com objecto, UGEA e data de abertura.
        val linhas = novos.take(8).map { c ->
            val abertura = if (c.dataAbertura.isNotBlank()) " — abre ${c.dataAbertura}" else ""
            val ugea = if (c.ugea.isNotBlank()) " (${c.ugea})" else ""
            "• ${c.objecto.take(90)}$ugea$abertura"
        }
        val corpo = buildString {
            append(linhas.joinToString("\n"))
            if (novos.size > 8) append("\n… e mais ${novos.size - 8}")
        }

        val estilo = NotificationCompat.InboxStyle().setBigContentTitle(titulo)
        linhas.forEach { estilo.addLine(it) }
        if (novos.size > 8) estilo.setSummaryText("+${novos.size - 8} outros")

        val notif = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(titulo)
            .setContentText(novos.firstOrNull()?.objecto?.take(90) ?: "Toque para ver")
            .setStyle(estilo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
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
