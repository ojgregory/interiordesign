package uk.ac.plymouth.interiordesign.Room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Colour::class], version = 1)
abstract class ColourDatabase : RoomDatabase() {
    abstract fun colourDao(): ColourDao
    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: ColourDatabase? = null

        fun getDatabase(context: Context): ColourDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ColourDatabase::class.java,
                    "colour_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }

        suspend fun fillColourDB(context: Context) {
            val colourDao = getDatabase(context).colourDao()
            colourDao.insert(
                Colour(
                    255,
                    0,
                    0,
                    255,
                    "RED"
                )
            )
            colourDao.insert(
                Colour(
                    0,
                    255,
                    0,
                    255,
                    "GREEN"
                )
            )
            colourDao.insert(
                Colour(
                    0,
                    0,
                    255,
                    255,
                    "BLUE"
                )
            )
            colourDao.insert(
                Colour(
                    255,
                    255,
                    0,
                    255,
                    "YELLOW"
                )
            )
            colourDao.insert(
                Colour(
                    255,
                    0,
                    255,
                    255,
                    "MAGENTA"
                )
            )
            colourDao.insert(
                Colour(
                    0,
                    255,
                    255,
                    255,
                    "CYAN"
                )
            )
            colourDao.insert(
                Colour(
                    255,
                    255,
                    255,
                    255,
                    "WHITE"
                )
            )
            colourDao.insert(
                Colour(
                    0,
                    0,
                    0,
                    255,
                    "BLACK"
                )
            )
        }
    }
}