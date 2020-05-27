package uk.ac.plymouth.interiordesign.Fillers

import android.renderscript.Allocation
import android.view.Surface
import uk.ac.plymouth.interiordesign.Room.Colour

// Filler interface to allow strategy pattern
interface Filler {
    var mInputAllocation: Allocation
    var mOutputAllocation: Allocation
    var mOriginalAllocation: Allocation
    var colour: Colour
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