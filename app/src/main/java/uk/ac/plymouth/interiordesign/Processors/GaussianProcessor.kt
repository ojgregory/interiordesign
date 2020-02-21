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


class GaussianProcessor(rs: RenderScript, dimensions: Size) {
    private var mInputAllocation: Allocation
    private var mKernelAllocation: Allocation
    private var mOutputAllocation: Allocation

    private var mProcessingHandler: Handler
    private var mGaussianTask: ProcessingTask
    private var mGaussianScript: ScriptC_gaussian

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

        mOutputAllocation = Allocation.createTyped(
            rs, rgbTypeBuilder.create(),
            Allocation.USAGE_IO_OUTPUT or Allocation.USAGE_SCRIPT
        )
        val processingThread = HandlerThread("GaussianProcessor")
        processingThread.start()
        mProcessingHandler = Handler(processingThread.looper)
        mGaussianScript = ScriptC_gaussian(rs)

        val gaussianCalculator = GaussianCalculator(1.5, 5)
        gaussianCalculator.createGaussianKernel()

        mKernelAllocation = Allocation.createSized(rs, Element.F32(rs), 5)
        mKernelAllocation.copyFrom(gaussianCalculator.kernel)
        mGaussianTask =
            ProcessingTask(
                mInputAllocation,
                mOutputAllocation,
                mKernelAllocation,
                mProcessingHandler,
                mGaussianScript,
                gaussianCalculator,
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
        private val mKernelAllocation: Allocation,
        private val mProcessingHandler: Handler,
        private val mGaussianScript: ScriptC_gaussian,
        private val mGaussianCalculator: GaussianCalculator,
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

            mGaussianScript._gCurrentFrame = mInputAllocation
            mGaussianScript._gImageW = mImageW
            mGaussianScript._gImageH = mImageH
            mGaussianScript._gMaskSize = mGaussianCalculator.mMaskSize
            mGaussianScript.bind_gConvMask1d(mKernelAllocation)
            mGaussianScript.forEach_convolve_kernel_row(mOutputAllocation)
            // Run processing pass
            mGaussianScript._gCurrentFrame = mOutputAllocation
            mGaussianScript.forEach_convolve_kernel_col(mOutputAllocation)
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