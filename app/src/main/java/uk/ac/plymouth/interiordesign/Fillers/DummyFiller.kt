package uk.ac.plymouth.interiordesign.Fillers

import android.renderscript.Allocation
import android.renderscript.RenderScript
import uk.ac.plymouth.interiordesign.Room.Colour
import uk.ac.plymouth.interiordesign.ScriptC_dummy

class DummyFiller(
    rs : RenderScript,
    override var mInputAllocation: Allocation,
    override var mOutputAllocation: Allocation, override var colour: Colour
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
        dummyScript._gCurrentFrame = mInputAllocation
        dummyScript.forEach_convertYToRGB(mOutputAllocation)
    }
}