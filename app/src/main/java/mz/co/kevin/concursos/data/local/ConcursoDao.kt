package mz.co.kevin.concursos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mz.co.kevin.concursos.data.model.CategoriaConcurso
import mz.co.kevin.concursos.data.model.Concurso
import kotlinx.coroutines.flow.Flow

@Dao
interface ConcursoDao {
    @Query(
        """
        SELECT * FROM concursos
        WHERE categoria = :categoria
        AND (:provincia = '' OR provincia = :provincia)
        AND (:termoBusca = '' OR objecto LIKE '%' || :termoBusca || '%' OR ugea LIKE '%' || :termoBusca || '%')
        ORDER BY timestampCaptura DESC
    """
    )
    fun observarConcursos(
        categoria: CategoriaConcurso,
        provincia: String,
        termoBusca: String
    ): Flow<List<Concurso>>

    @Query(
        """
        SELECT DISTINCT provincia FROM concursos
        WHERE categoria = :categoria AND TRIM(provincia) != ''
        ORDER BY provincia ASC
    """
    )
    fun observarProvincias(categoria: CategoriaConcurso): Flow<List<String>>

    @Query("SELECT referencia FROM concursos")
    suspend fun obterTodasReferencias(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirOuAtualizar(concursos: List<Concurso>)
}
