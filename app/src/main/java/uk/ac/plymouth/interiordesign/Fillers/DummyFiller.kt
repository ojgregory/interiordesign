package uk.ac.plymouth.interiordesign.Fillers

import android.renderscript.Allocation
import android.renderscript.RenderScript
import uk.ac.plymouth.interiordesign.ScriptC_dummy

class DummyFiller(
    rs : RenderScript,
    override var mInputAllocation: Allocation,
    override var mOutputAllocation: Allocation
) : Filler {
    private val dummyScript : ScriptC_dummy = ScriptC_dummy(rs)

    override fun run() {
        dummyScript._gCurrentFrame = mInputAllocation
        dummyScript.forEach_convertYToRGB(mOutputAllocation)
    }
}