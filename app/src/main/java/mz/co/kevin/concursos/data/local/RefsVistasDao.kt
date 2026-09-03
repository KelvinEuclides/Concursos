package mz.co.kevin.concursos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mz.co.kevin.concursos.data.model.RefVista

@Dao
interface RefsVistasDao {
    @Query("SELECT referencia FROM refs_vistas")
    suspend fun todas(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun marcar(itens: List<RefVista>)

    /** Evita crescimento sem limite: mantém só as N referências mais recentes. */
    @Query(
        """
        DELETE FROM refs_vistas WHERE referencia NOT IN (
            SELECT referencia FROM refs_vistas ORDER BY vistaEm DESC LIMIT :manter
        )
    """
    )
    suspend fun podar(manter: Int)
}
