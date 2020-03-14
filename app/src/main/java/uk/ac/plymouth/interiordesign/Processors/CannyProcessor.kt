package uk.ac.plymouth.interiordesign.Processors

import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import android.util.Size
import uk.ac.plymouth.interiordesign.ScriptC_canny


class CannyProcessor(
    rs: RenderScript, private val dimensions: Size,
    override var mInputAllocation: Allocation,
    override var mOutputAllocation: Allocation,
    private var highThreshold : Int,
    private var lowThreshold : Int
) : Processor {

    private var mCannyScript: ScriptC_canny
    private var mOperatorAllocationX : Allocation
    private var mOperatorAllocationY : Allocation
    private val sobelOperatorProvider = SobelOperatorProvider()
    private var mProcessingAllocation : Allocation
    private var mGxAllocation : Allocation
    private var mGyAllocation : Allocation
    private var mTempAllocation : Allocation

    init {
        mCannyScript = ScriptC_canny(rs)
        mOperatorAllocationX = Allocation.createSized(rs, Element.I32(rs), sobelOperatorProvider.maskSize * sobelOperatorProvider.maskSize)
        mOperatorAllocationX.copyFrom(sobelOperatorProvider.getOperatorX())

        mOperatorAllocationY = Allocation.createSized(rs, Element.I32(rs), sobelOperatorProvider.maskSize * sobelOperatorProvider.maskSize)
        mOperatorAllocationY.copyFrom(sobelOperatorProvider.getOperatorY())

        mProcessingAllocation = Allocation.createTyped(rs, Type.createXY(rs,Element.F32_2(rs), dimensions.width, dimensions.height))
        mTempAllocation = Allocation.createTyped(rs, Type.createXY(rs,Element.F32_2(rs), dimensions.width, dimensions.height))
        mGxAllocation = Allocation.createTyped(rs, Type.createXY(rs,Element.F32(rs), dimensions.width, dimensions.height))
        mGyAllocation = Allocation.createTyped(rs, Type.createXY(rs,Element.F32(rs), dimensions.width, dimensions.height))
    }

    fun changeOperators(selectedOperator : Int) {
        sobelOperatorProvider.selectedOperator = selectedOperator
        var operatorX = sobelOperatorProvider.getOperatorX()
        mOperatorAllocationX.copyFrom(sobelOperatorProvider.getOperatorX())
        mOperatorAllocationY.copyFrom(sobelOperatorProvider.getOperatorY())
    }


    override fun run() {
        mCannyScript._gCurrentFrame = mInputAllocation
        mCannyScript._gImageW = dimensions.width
        mCannyScript._gImageH = dimensions.height
        mCannyScript._maskSize = sobelOperatorProvider.maskSize
        mCannyScript._Gx = mGxAllocation
        mCannyScript._Gy = mGyAllocation
        mCannyScript._highThreshold = highThreshold
        mCannyScript._lowThreshold = lowThreshold
        mCannyScript.bind_convXMask(mOperatorAllocationX)
        mCannyScript.bind_convYMask(mOperatorAllocationY)
        // Run processing pass
        mCannyScript.forEach_convolveKernel(mProcessingAllocation)
        mCannyScript._input = mProcessingAllocation
        mCannyScript.forEach_supression(mTempAllocation)
        mCannyScript._input = mTempAllocation
        mCannyScript.forEach_doubleThreshold(mProcessingAllocation)
        mCannyScript._input = mProcessingAllocation
        mCannyScript.forEach_hystersis(mOutputAllocation)
    }

}