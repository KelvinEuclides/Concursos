package mz.co.kevin.concursos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "concursos")
data class Concurso(
    @PrimaryKey val referencia: String,
    val modalidade: String,
    val objecto: String,
    val ugea: String,
    val provincia: String,
    val dataLancamento: String,
    val dataAbertura: String,
    val linkDetalhes: String,
    val categoria: CategoriaConcurso,
    val ehInformatica: Boolean,
    val timestampCaptura: Long = System.currentTimeMillis()
)
