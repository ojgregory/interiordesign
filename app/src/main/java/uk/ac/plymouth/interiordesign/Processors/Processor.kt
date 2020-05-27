package uk.ac.plymouth.interiordesign.Processors

import android.graphics.ImageFormat
import android.media.ImageReader
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import android.util.Size
import android.view.Surface

// Interface for processors, Strategy Pattern
interface Processor {
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