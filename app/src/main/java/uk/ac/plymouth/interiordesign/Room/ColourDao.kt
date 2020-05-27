package uk.ac.plymouth.interiordesign.Room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

// Wraps queries with kotlin class and annotation
@Dao
interface ColourDao {
    @Query("SELECT * FROM colour")
    suspend fun getAll(): List<Colour>

    @Insert
    suspend fun insertAll(vararg colours: Colour)

    @Insert
    suspend fun insert(colour : Colour)

    @Query("DELETE FROM colour")
    suspend fun deleteAll()
}