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

    /**
     * Lê a categoria a partir do texto guardado. Um valor desconhecido (dados
     * antigos ou corrompidos) cai em [CategoriaConcurso.ABERTO] em vez de
     * rebentar a query com [IllegalArgumentException].
     */
    @TypeConverter
    fun textoParaCategoria(valor: String): CategoriaConcurso =
        runCatching { CategoriaConcurso.valueOf(valor) }.getOrDefault(CategoriaConcurso.ABERTO)
}
