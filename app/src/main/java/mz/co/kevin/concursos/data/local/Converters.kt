package mz.co.kevin.concursos.data.local

import androidx.room.TypeConverter
import mz.co.kevin.concursos.data.model.CategoriaConcurso

/**
 * Room não sabe persistir enums automaticamente. Guardamos a [CategoriaConcurso]
 * pelo seu nome e reconstruímos na leitura.
 */
class Converters {
    @TypeConverter
    fun categoriaParaTexto(categoria: CategoriaConcurso): String = categoria.name

    @TypeConverter
    fun textoParaCategoria(valor: String): CategoriaConcurso = CategoriaConcurso.valueOf(valor)
}
