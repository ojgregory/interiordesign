package uk.ac.plymouth.interiordesign

import android.graphics.Color

class Colour(var r: Int, var g: Int, var b: Int, var a: Int, var name: String) {
    var rgba : Int = 0
    init {
        updateRGBA()
    }

    fun updateRGBA() {
        rgba = Color.argb(a, r, g, b)
    }
}