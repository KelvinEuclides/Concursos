package mz.co.kevin.concursos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Concurso marcado pelo utilizador. É uma cópia independente da tabela `concursos`
 * (que é substituída a cada sincronização), por isso sobrevive mesmo que o edital
 * saia do portal. Guarda as datas de submissão para exportar ao calendário.
 */
@Entity(tableName = "concursos_guardados")
data class ConcursoGuardado(
    @PrimaryKey val referencia: String,
    val modalidade: String,
    val objecto: String,
    val ugea: String,
    val provincia: String,
    val categoria: CategoriaConcurso,
    val ehInformatica: Boolean,
    val dataInicioSubmissao: String,
    val dataFimSubmissao: String,
    val linkDetalhes: String,
    val requisitos: String = "",
    val timestampGuardado: Long = System.currentTimeMillis()
)
