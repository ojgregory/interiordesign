package uk.ac.plymouth.interiordesign.Fillers

import android.renderscript.Allocation
import android.renderscript.RenderScript
import uk.ac.plymouth.interiordesign.Room.Colour
import uk.ac.plymouth.interiordesign.ScriptC_dummy

// This applies no processing just copies from original to output
// Whilst converting from YUV to RGBA
class DummyFiller(
    rs : RenderScript,
    override var mInputAllocation: Allocation,
    override var mOutputAllocation: Allocation,
    override var mOriginalAllocation: Allocation,
    override var colour: Colour,
    var showColour : Boolean
) : Filler {
    private val dummyScript : ScriptC_dummy = ScriptC_dummy(rs)
    override var x: Int = 0
        get() = field
        set(value) {
            field = value
        }
    override var y: Int = 0
        get() = field
        set(value) {
            field = value
        }

    override fun run() {
        dummyScript._gCurrentFrame = mOriginalAllocation
        if (showColour)
            dummyScript.forEach_convertYToRGB_colour(mOutputAllocation)
        else
            dummyScript.forEach_convertYToRGB(mOutputAllocation)
    }
}