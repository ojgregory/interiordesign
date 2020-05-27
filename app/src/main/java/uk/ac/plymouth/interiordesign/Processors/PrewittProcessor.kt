package uk.ac.plymouth.interiordesign.Processors

import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.util.Size
import uk.ac.plymouth.interiordesign.ScriptC_sobel

// Apply Prewitt Edge Detection, using generic convolution
// Based on Sobel implementation
class PrewittProcessor(
    rs: RenderScript, private val dimensions: Size,
    override var mInputAllocation: Allocation,
    override var mOutputAllocation: Allocation
) : Processor {

    private var mSobelScript: ScriptC_sobel = ScriptC_sobel(rs)
    private var mOperatorAllocationX: Allocation
    private var mOperatorAllocationY: Allocation
    private val operatorX = intArrayOf(1, 0, -1, 1, 0, -1, 1, 0, -1)
    private val operatorY = intArrayOf(1, 1, 1, 0, 0, 0, -1, -1, -1)
    private val maskSize = 3


    init {
        mOperatorAllocationX = Allocation.createSized(
            rs,
            Element.I32(rs),
            maskSize * maskSize
        )
        mOperatorAllocationX.copyFrom(operatorX)

        mOperatorAllocationY = Allocation.createSized(
            rs,
            Element.I32(rs),
            maskSize * maskSize
        )
        mOperatorAllocationY.copyFrom(operatorY)
    }

    override fun run() {
        mSobelScript._gCurrentFrame = mInputAllocation
        mSobelScript._gImageW = dimensions.width
        mSobelScript._gImageH = dimensions.height
        mSobelScript._maskSize = maskSize
        mSobelScript.bind_convXMask(mOperatorAllocationX)
        mSobelScript.bind_convYMask(mOperatorAllocationY)
        // Run processing pass
        mSobelScript.forEach_convolveKernel(mOutputAllocation)
    }
}