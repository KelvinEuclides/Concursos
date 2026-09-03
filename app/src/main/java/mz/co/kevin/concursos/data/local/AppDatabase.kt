package mz.co.kevin.concursos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.ConcursoGuardado
import mz.co.kevin.concursos.data.model.FornecedorCef
import mz.co.kevin.concursos.data.model.RefVista

@Database(
    entities = [Concurso::class, FornecedorCef::class, ConcursoGuardado::class, RefVista::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun concursoDao(): ConcursoDao
    abstract fun fornecedorDao(): FornecedorDao
    abstract fun guardadosDao(): GuardadosDao
    abstract fun refsVistasDao(): RefsVistasDao
}
