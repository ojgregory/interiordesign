package uk.ac.plymouth.interiordesign.Processors

import android.graphics.ImageFormat
import android.media.Image
import android.media.ImageReader
import android.media.ImageWriter
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import uk.ac.plymouth.interiordesign.ScriptC_sobel


class SobelProcessor(
    rs: RenderScript, private val dimensions: Size,
    override var mInputAllocation: Allocation,
    override var mOutputAllocation: Allocation
) : Processor {

    private var mSobelScript: ScriptC_sobel


    init {
        mSobelScript = ScriptC_sobel(rs)
    }


    override fun run() {
        mSobelScript._gCurrentFrame = mInputAllocation
        mSobelScript._gImageW = dimensions.width
        mSobelScript._gImageH = dimensions.height
        // Run processing pass
        mSobelScript.forEach_convolveKernel(mOutputAllocation)
    }

}