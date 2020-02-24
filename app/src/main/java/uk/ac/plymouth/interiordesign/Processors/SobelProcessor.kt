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
import uk.ac.plymouth.interiordesign.ScriptC_sobel


class SobelProcessor(rs: RenderScript, dimensions: Size) {
    private var mInputAllocation: Allocation
    private var mPrevAllocation: Allocation
    private var mOutputAllocation: Allocation

    private var mProcessingHandler: Handler
    private var mSobelTask: ProcessingTask
    private var mSobelScript: ScriptC_sobel

    init {
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
        rgbTypeBuilder.setX(dimensions.width)
        rgbTypeBuilder.setY(dimensions.height)
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
        mSobelTask =
            ProcessingTask(
                mInputAllocation,
                mOutputAllocation,
                mProcessingHandler,
                mSobelScript,
                dimensions.width,
                dimensions.height
            )
    }

    /**
     * Simple class to keep track of incoming frame count,
     * and to process the newest one in the processing thread
     */
    internal class ProcessingTask(
        private val mInputAllocation: Allocation,
        private val mOutputAllocation: Allocation,
        private val mProcessingHandler: Handler,
        private val mSobelScript: ScriptC_sobel,
        imageW: Int,
        imageH: Int
    ) :
        Runnable, Allocation.OnBufferAvailableListener {
        private val mImageW = imageW
        private val mImageH = imageH
        private var mPendingFrames = 0
        override fun onBufferAvailable(a: Allocation?) {
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

            mSobelScript._gCurrentFrame = mInputAllocation
            mSobelScript._gImageW = mImageW
            mSobelScript._gImageH = mImageH
            // Run processing pass
            mSobelScript.forEach_convolveKernel(mOutputAllocation)
            mOutputAllocation.ioSend()
        }

        init {
            mInputAllocation.setOnBufferAvailableListener(this);
        }
    }

    fun getInputNormalSurface(): Surface {
        return mInputAllocation.surface
    }

    fun setOutputSurface(output: Surface?) {
        mOutputAllocation.surface = output
    }
}