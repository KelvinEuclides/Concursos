package mz.co.kevin.concursos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Registo de concursos que a app já "viu" — seja porque o utilizador os abriu/leu
 * na app, seja porque já foram incluídos numa notificação. É consultado antes de
 * notificar, para nunca repetir um alerta de um concurso já conhecido.
 *
 * Tabela independente de `concursos` (que é reescrita a cada sincronização), por
 * isso o histórico de "vistos" sobrevive às limpezas.
 */
@Entity(tableName = "refs_vistas")
data class RefVista(
    @PrimaryKey val referencia: String,
    val vistaEm: Long = System.currentTimeMillis()
)
