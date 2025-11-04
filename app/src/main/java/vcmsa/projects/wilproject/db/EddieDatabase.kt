package vcmsa.projects.wilproject.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import vcmsa.projects.wilproject.DateConverter
import vcmsa.projects.wilproject.models.Notes
import vcmsa.projects.wilproject.dao.NotesDao
import vcmsa.projects.wilproject.models.User
import vcmsa.projects.wilproject.dao.UserDao
import vcmsa.projects.wilproject.dao.CalendarDao
import vcmsa.projects.wilproject.dao.ScoreDao
import vcmsa.projects.wilproject.models.CalendarSchedule
import vcmsa.projects.wilproject.models.ScoreEntity
//This class defines the database schema locally (Philipp Lackner,2022)
@Database(entities = [User::class, Notes::class, CalendarSchedule::class, ScoreEntity::class], version = 3, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class EddieDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun notesDao(): NotesDao
    abstract fun calenderDao(): CalendarDao

    abstract fun scoreDao(): ScoreDao


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