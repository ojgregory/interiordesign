package uk.ac.plymouth.interiordesign.Fillers

import android.renderscript.Allocation
import android.view.Surface

interface Filler {
    var mInputAllocation: Allocation
    var mOutputAllocation: Allocation

    fun run()

    fun getInputSurface() : Surface {
        return mInputAllocation.surface
    }

    fun setOutputSurface(output : Surface?) {
        mOutputAllocation.surface = output
    }
}