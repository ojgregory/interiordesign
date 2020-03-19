package uk.ac.plymouth.interiordesign.Processors

import android.renderscript.Allocation
import android.renderscript.RenderScript
import android.renderscript.Script
import uk.ac.plymouth.interiordesign.ScriptC_dummy

class DummyPreprocessor(
    rs : RenderScript,
    override var mInputAllocation: Allocation,
    override var mOutputAllocation: Allocation,
    override var mTempAllocation: Allocation
) : PreProcessor {
    private val mDummyScriptC = ScriptC_dummy(rs)

    override fun run() {
        mDummyScriptC._gCurrentFrame = mInputAllocation
        mDummyScriptC.forEach_dummy_preprocess(mOutputAllocation)
    }

}