package vcmsa.projects.wilproject

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [User::class, Notes::class, CalendarSchedule::class], version = 2, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class EddieDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun notesDao(): NotesDao
    abstract fun calenderDao(): CalendarDao

    companion object {
        @Volatile
        private var INSTANCE: EddieDatabase? = null

        fun getDatabase(context: Context): EddieDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EddieDatabase::class.java,
                    "eddieDB.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}