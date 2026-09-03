package mz.co.kevin.concursos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mz.co.kevin.concursos.data.model.FornecedorCef
import kotlinx.coroutines.flow.Flow

@Dao
interface FornecedorDao {
    @Query(
        """
        SELECT * FROM fornecedores_cef
        WHERE (:provincia = '' OR provincia LIKE '%' || :provincia || '%')
        AND (:termo = '' OR nome LIKE '%' || :termo || '%' OR nuit LIKE '%' || :termo || '%' OR actividades LIKE '%' || :termo || '%')
        ORDER BY nome ASC
    """
    )
    fun observarFornecedores(provincia: String, termo: String): Flow<List<FornecedorCef>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarFornecedores(fornecedores: List<FornecedorCef>)
}
