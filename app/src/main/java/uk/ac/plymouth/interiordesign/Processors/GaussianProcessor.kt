package uk.ac.plymouth.interiordesign.Processors

import android.graphics.ImageFormat
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import uk.ac.plymouth.interiordesign.ScriptC_gaussian


class GaussianProcessor(
    private var rs: RenderScript, private val dimensions: Size,
    override var mInputAllocation: Allocation,
    override var mOutputAllocation: Allocation,
    override var mTempAllocation: Allocation,
    sigma: Double,
    maskSize: Int
) : PreProcessor {
    private var mKernelAllocation: Allocation
    private var gaussianCalculator: GaussianCalculator = GaussianCalculator(sigma, maskSize)
    var sigma = sigma
        get() = field
        set(value) {
            field = value
            gaussianCalculator.mSigma = sigma
        }
    var maskSize = maskSize
        get() = field
        set(value) {
            field = value
            gaussianCalculator.mMaskSize = maskSize
        }
    private var mGaussianScript: ScriptC_gaussian = ScriptC_gaussian(rs)

    fun recalculateGaussianKernel() {
        gaussianCalculator.createGaussianKernel()
        mKernelAllocation = Allocation.createSized(rs, Element.F32(rs), maskSize)
        mKernelAllocation.copyFrom(gaussianCalculator.kernel)
    }

    init {

        gaussianCalculator.createGaussianKernel()
        mKernelAllocation = Allocation.createSized(rs, Element.F32(rs), maskSize)
        mKernelAllocation.copyFrom(gaussianCalculator.kernel)
    }

    override fun run() {
        mGaussianScript._gCurrentFrame = mInputAllocation
        mGaussianScript._gImageW = dimensions.width
        mGaussianScript._gImageH = dimensions.height
        mGaussianScript._gMaskSize = gaussianCalculator.mMaskSize
        mGaussianScript.bind_gConvMask1d(mKernelAllocation)

        // Convolve image in horizontal direction
        mGaussianScript.forEach_convolve_kernel_row(mTempAllocation)
        mGaussianScript._gCurrentFrame = mTempAllocation
        // Use previous output to convolve in vertical direction providing final output
        mGaussianScript.forEach_convolve_kernel_col(mOutputAllocation)
    }
}