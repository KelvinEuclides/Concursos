package mz.co.kevin.concursos.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import mz.co.kevin.concursos.data.model.Concurso
import mz.co.kevin.concursos.data.model.ConcursoGuardado
import mz.co.kevin.concursos.data.model.FornecedorCef
import mz.co.kevin.concursos.data.model.RefVista

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE concursos ADD COLUMN fonte TEXT NOT NULL DEFAULT 'UFSA'")
        db.execSQL("ALTER TABLE concursos_guardados ADD COLUMN fonte TEXT NOT NULL DEFAULT 'UFSA'")
    }
}

@Database(
    entities = [Concurso::class, FornecedorCef::class, ConcursoGuardado::class, RefVista::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun concursoDao(): ConcursoDao
    abstract fun fornecedorDao(): FornecedorDao
    abstract fun guardadosDao(): GuardadosDao
    abstract fun refsVistasDao(): RefsVistasDao
}
