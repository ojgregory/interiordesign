package uk.ac.plymouth.interiordesign.Processors

import android.renderscript.Allocation
import android.renderscript.RenderScript
import android.util.Size
import uk.ac.plymouth.interiordesign.ScriptC_bilateral

class BilateralFilterProcessor(
    var rs : RenderScript,
    var dimensions: Size,
    override var mInputAllocation: Allocation,
    override var mOutputAllocation: Allocation,
    override var mTempAllocation: Allocation,
    sigmaRange : Float,
    sigmaSpatial : Float,
    diameter: Int
) : PreProcessor {

    private var sigmaRange = sigmaRange
        get() = field
        set(value) {
            field = value
        }

    private var sigmaSpatial = sigmaSpatial
        get() = field
        set(value) {
            field = value
        }
    private var diameter = diameter
        get() = field
        set(value) {
            field = value
        }

    private var mBilateralScript: ScriptC_bilateral = ScriptC_bilateral(rs)

    override fun run() {
        mBilateralScript._gCurrentFrame = mInputAllocation
        mBilateralScript._gImageW = dimensions.width
        mBilateralScript._gImageH = dimensions.height
        mBilateralScript._sigmaRange = sigmaRange
        mBilateralScript._sigmaSpatial = sigmaSpatial
        mBilateralScript._diameter = diameter
        mBilateralScript.forEach_applyBilateralFilter(mOutputAllocation)
    }
}