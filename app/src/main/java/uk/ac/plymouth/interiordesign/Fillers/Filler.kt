package uk.ac.plymouth.interiordesign.Fillers

import android.renderscript.Allocation
import android.view.Surface

interface Filler {
    var mInputAllocation: Allocation
    var mOutputAllocation: Allocation
    var x : Int
    var y : Int

    fun run()

    fun getInputSurface() : Surface {
        return mInputAllocation.surface
    }

    fun setOutputSurface(output : Surface?) {
        mOutputAllocation.surface = output
    }
}