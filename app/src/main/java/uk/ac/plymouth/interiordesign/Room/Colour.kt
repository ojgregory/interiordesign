package uk.ac.plymouth.interiordesign.Room

import android.graphics.Color
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Colour(
    @ColumnInfo var r: Int,
    @ColumnInfo var g: Int,
    @ColumnInfo var b: Int,
    @ColumnInfo var a: Int,
    @PrimaryKey var name: String) {
    var rgba : Int = 0
    init {
        updateRGBA()
    }

    fun updateRGBA() {
        rgba = Color.argb(a, r, g, b)
    }
}