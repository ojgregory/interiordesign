package uk.ac.plymouth.interiordesign.Processors

import android.renderscript.Allocation
import android.view.Surface

interface PreProcessor {
    var mInputAllocation: Allocation
    var mOutputAllocation: Allocation
    var mTempAllocation: Allocation

    fun run()

    fun getInputSurface() : Surface {
        return mInputAllocation.surface
    }

    fun setOutputSurface(output : Surface?) {
        mOutputAllocation.surface = output
    }
}