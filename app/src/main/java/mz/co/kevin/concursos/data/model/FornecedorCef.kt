package mz.co.kevin.concursos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fornecedores_cef")
data class FornecedorCef(
    @PrimaryKey val certificado: String,
    val nome: String,
    val nuit: String,
    val provincia: String,
    val dataInscricao: String,
    val actividades: String,
    /** URL absoluta de `inscritoscef_detalhes.php` para este fornecedor (ramos, contactos, regime). */
    val linkDetalhes: String = "",
    val timestampCaptura: Long = System.currentTimeMillis()
)
