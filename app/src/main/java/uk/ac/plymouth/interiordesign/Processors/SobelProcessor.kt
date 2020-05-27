package uk.ac.plymouth.interiordesign.Processors

import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.util.Size
import uk.ac.plymouth.interiordesign.ScriptC_sobel

// Use either sobel or scharr operator
// Convolve with input
class SobelProcessor(
    rs: RenderScript, private val dimensions: Size,
    override var mInputAllocation: Allocation,
    override var mOutputAllocation: Allocation
) : Processor {

    private var mSobelScript: ScriptC_sobel
    private var mOperatorAllocationX : Allocation
    private var mOperatorAllocationY : Allocation
    private val sobelOperatorProvider = SobelOperatorProvider()


    init {
        mSobelScript = ScriptC_sobel(rs)
        mOperatorAllocationX = Allocation.createSized(rs, Element.I32(rs), sobelOperatorProvider.maskSize * sobelOperatorProvider.maskSize)
        mOperatorAllocationX.copyFrom(sobelOperatorProvider.getOperatorX())

        mOperatorAllocationY = Allocation.createSized(rs, Element.I32(rs), sobelOperatorProvider.maskSize * sobelOperatorProvider.maskSize)
        mOperatorAllocationY.copyFrom(sobelOperatorProvider.getOperatorY())
    }

    fun changeOperators(selectedOperator : Int) {
        sobelOperatorProvider.selectedOperator = selectedOperator
        mOperatorAllocationX.copyFrom(sobelOperatorProvider.getOperatorX())
        mOperatorAllocationY.copyFrom(sobelOperatorProvider.getOperatorY())
    }


    override fun run() {
        mSobelScript._gCurrentFrame = mInputAllocation
        mSobelScript._gImageW = dimensions.width
        mSobelScript._gImageH = dimensions.height
        mSobelScript._maskSize = sobelOperatorProvider.maskSize
        mSobelScript.bind_convXMask(mOperatorAllocationX)
        mSobelScript.bind_convYMask(mOperatorAllocationY)
        // Run processing pass
        mSobelScript.forEach_convolveKernel(mOutputAllocation)
    }

}