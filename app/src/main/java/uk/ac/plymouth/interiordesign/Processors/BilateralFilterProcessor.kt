package uk.ac.plymouth.interiordesign.Processors

import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import android.util.Size
import uk.ac.plymouth.interiordesign.ScriptC_bilateral
import kotlin.math.roundToInt

class BilateralFilterProcessor(
    var rs : RenderScript,
    var dimensions: Size,
    override var mInputAllocation: Allocation,
    override var mOutputAllocation: Allocation,
    override var mTempAllocation: Allocation,
    sigmaRange : Float,
    sigmaSpatial : Float,
    diameter: Int,
    private var ncomps: Int,
    private var spacial_stdev: Float,
    private var range_stdev: Float
) : PreProcessor {

    private var mKIndexAllocation : Allocation
    private var mKFractAllocation : Allocation
    private var mSpatialAllocation : Allocation
    private var mRangeAllocation : Allocation
    private var mNCAllocation : Allocation
    private var mPBCAllocation : Allocation
    private var mPBCTempAllocation : Allocation
    private val spatialSize = (2 * spacial_stdev + 1).roundToInt();

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

    init {
        mNCAllocation = Allocation.createSized(rs, Element.F32(rs), ncomps)
        mKIndexAllocation = Allocation.createSized(rs, Element.F32(rs), 256)
        mKFractAllocation = Allocation.createSized(rs, Element.F32(rs), 256)
        mRangeAllocation = Allocation.createSized(rs, Element.F32(rs), 256)

        mSpatialAllocation = Allocation.createSized(rs, Element.F32(rs), spatialSize)

        mBilateralScript._spatial_size = spatialSize
        mBilateralScript._spatial_stdev = spacial_stdev
        mBilateralScript._range_stdev = range_stdev
        mBilateralScript._maxval = 255
        mBilateralScript._minval = 0
        mBilateralScript._ncomps = ncomps
        mBilateralScript.bind_kfract(mKFractAllocation)
        mBilateralScript.bind_kindex(mKIndexAllocation)

        mBilateralScript.forEach_generateKValues(mNCAllocation)
        mBilateralScript.bind_nc(mNCAllocation)
        mBilateralScript.invoke_generateKIndex()
        mBilateralScript.invoke_generateKFract()
        mBilateralScript._denom = (2.0 * range_stdev * range_stdev).toFloat()
        mBilateralScript.forEach_calculateRangeKernel(mRangeAllocation)
        mBilateralScript._denom = (2.0 * spacial_stdev * spacial_stdev).toFloat()
        mBilateralScript.forEach_calculateSpatialKernel(mSpatialAllocation)

        mPBCAllocation = Allocation.createTyped(rs, Type.createXYZ(rs, Element.I32(rs), dimensions.width, dimensions.height, ncomps))
        mPBCTempAllocation = Allocation.createTyped(rs, Type.createXYZ(rs, Element.I32(rs), dimensions.width, dimensions.height, ncomps))
    }

    override fun run() {
        mBilateralScript._spatial_size = spatialSize
        mBilateralScript._spatial_stdev = spacial_stdev
        mBilateralScript._range_stdev = range_stdev
        mBilateralScript._maxval = 255
        mBilateralScript._minval = 0
        mBilateralScript._ncomps = ncomps
        mBilateralScript.bind_kfract(mKFractAllocation)
        mBilateralScript.bind_kindex(mKIndexAllocation)
        mBilateralScript.bind_nc(mNCAllocation)
        mBilateralScript.bind_range(mRangeAllocation)
        mBilateralScript.bind_spatial(mSpatialAllocation)
        mBilateralScript._gCurrentFrame = mInputAllocation

        mBilateralScript.forEach_horizontalConvolution(mPBCTempAllocation)
        mBilateralScript.forEach_verticalConvolution(mPBCAllocation)
    }

    fun runOld() {
        mBilateralScript._gCurrentFrame = mInputAllocation
        mBilateralScript._gImageW = dimensions.width
        mBilateralScript._gImageH = dimensions.height
        mBilateralScript._sigmaRange = sigmaRange
        mBilateralScript._sigmaSpatial = sigmaSpatial
        mBilateralScript._diameter = diameter
        mBilateralScript.forEach_applyBilateralFilter(mOutputAllocation)
    }
}