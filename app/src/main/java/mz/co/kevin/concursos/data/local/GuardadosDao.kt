package mz.co.kevin.concursos.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import mz.co.kevin.concursos.data.model.ConcursoGuardado
import kotlinx.coroutines.flow.Flow

@Dao
interface GuardadosDao {
    @Query("SELECT * FROM concursos_guardados ORDER BY timestampGuardado DESC")
    fun observarGuardados(): Flow<List<ConcursoGuardado>>

    @Query("SELECT referencia FROM concursos_guardados")
    fun observarReferencias(): Flow<List<String>>

    @Upsert
    suspend fun guardar(item: ConcursoGuardado)

    @Query("UPDATE concursos_guardados SET requisitos = :requisitos WHERE referencia = :referencia")
    suspend fun atualizarRequisitos(referencia: String, requisitos: String)

    @Query("DELETE FROM concursos_guardados WHERE referencia = :referencia")
    suspend fun remover(referencia: String)
}
