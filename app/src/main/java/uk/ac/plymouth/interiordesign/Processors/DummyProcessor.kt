package uk.ac.plymouth.interiordesign.Processors

import android.renderscript.Allocation
import android.renderscript.RenderScript
import uk.ac.plymouth.interiordesign.ScriptC_dummy

// Transfers the input to the output, with no processing
class DummyProcessor(
    rs : RenderScript,
    override var mInputAllocation: Allocation,
    override var mOutputAllocation: Allocation
) : Processor {
    private val dummyScript : ScriptC_dummy = ScriptC_dummy(rs)

    override fun run() {
        dummyScript._gCurrentFrame = mInputAllocation
        dummyScript.forEach_dummy_preprocess(mOutputAllocation)
    }
}