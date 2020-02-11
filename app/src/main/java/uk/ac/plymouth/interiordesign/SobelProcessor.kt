package uk.ac.plymouth.interiordesign

import android.graphics.ImageFormat
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import androidx.renderscript.Allocation
import androidx.renderscript.Element
import androidx.renderscript.RenderScript
import androidx.renderscript.Type


class SobelProcessor {
    private lateinit var mInputAllocation: Allocation
    private lateinit var mPrevAllocation: Allocation
    private lateinit var mOutputAllocation: Allocation

    private lateinit var mProcessingHandler: Handler
    private lateinit var mSobelTask: ProcessingTask
    private lateinit var mSobelScript: ScriptC_sobel

    fun ViewfinderProcessor(rs: RenderScript, dimensions: Size) {
        val yuvTypeBuilder = Type.Builder(rs, Element.createPixel(rs,
            Element.DataType.UNSIGNED_8, Element.DataKind.PIXEL_YUV))
        yuvTypeBuilder.setX(dimensions.getWidth())
        yuvTypeBuilder.setY(dimensions.getHeight())
        yuvTypeBuilder.setYuvFormat(ImageFormat.YUV_420_888)
        mInputAllocation = Allocation.createTyped(
            rs, yuvTypeBuilder.create(),
            Allocation.USAGE_IO_INPUT or Allocation.USAGE_SCRIPT
        )

        val rgbTypeBuilder = Type.Builder(rs, Element.RGBA_8888(rs))
        rgbTypeBuilder.setX(dimensions.getWidth())
        rgbTypeBuilder.setY(dimensions.getHeight())
        mPrevAllocation = Allocation.createTyped(
            rs, rgbTypeBuilder.create(),
            Allocation.USAGE_SCRIPT
        )
        mOutputAllocation = Allocation.createTyped(
            rs, rgbTypeBuilder.create(),
            Allocation.USAGE_IO_OUTPUT or Allocation.USAGE_SCRIPT
        )
        val processingThread = HandlerThread("SobelProcessor")
        processingThread.start()
        mProcessingHandler = Handler(processingThread.looper)
        mSobelScript = ScriptC_sobel(rs)
        mSobelScript.set_gPrevFrame(mPrevAllocation)
        mSobelTask = ProcessingTask(mInputAllocation, mPrevAllocation, mOutputAllocation, mProcessingHandler, mSobelScript,dimensions.width, dimensions.height)
    }

    /**
     * Simple class to keep track of incoming frame count,
     * and to process the newest one in the processing thread
     */
    internal class ProcessingTask(
        private val mInputAllocation: Allocation,
        private val mPrevAllocation: Allocation,
        private val mOutputAllocation: Allocation,
        private val mProcessingHandler: Handler,
        private val mSobelScript: ScriptC_sobel,
        imageW: Int,
        imageH: Int
    ) :
        Runnable {
        private val mImageW = imageW
        private val mImageH = imageW
        private var mPendingFrames = 0
        private var mFrameCounter = 0
        fun onBufferAvailable(a: Allocation?) {
            synchronized(this) {
                mPendingFrames++
                mProcessingHandler.post(this)
            }
        }

        override fun run() { // Find out how many frames have arrived
            var pendingFrames: Int
            synchronized(this) {
                pendingFrames = mPendingFrames
                mPendingFrames = 0
                // Discard extra messages in case processing is slower than frame rate
                mProcessingHandler.removeCallbacks(this)
            }
            // Get to newest input
            for (i in 0 until pendingFrames) {
                mInputAllocation.ioReceive()
            }

            mSobelScript.bind_gPixels(mInputAllocation);
            mSobelScript.set_gFrameCounter(mFrameCounter++)
            mSobelScript.set_gCurrentFrame(mInputAllocation)
            mSobelScript.set_gImageW(mImageW)
            mSobelScript.set_gImageH(mImageH)
            // Run processing pass
            mSobelScript.forEach_convolveKernel(mPrevAllocation, mOutputAllocation)
            mOutputAllocation.ioSend()
        }

        init {
        }
    }

    private fun SobelKotlin() {

    }

    fun getInputNormalSurface(): Surface {
    }

    fun setOutputSurface(output: Surface?) {
        mOutputAllocation.setSurface(output)
    }
}