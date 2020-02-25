package uk.ac.plymouth.interiordesign.Processors

import android.graphics.ImageFormat
import android.os.Handler
import android.os.HandlerThread
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import android.util.Size
import android.view.Surface

class ProcessingCoordinator(
    preProcessorChoice: Int,
    processorChoice: Int,
    private val rs: RenderScript,
    private val dimensions: Size
) {
    private lateinit var preProcessor: PreProcessor
    private lateinit var processor: Processor
    private var inputAllocation: Allocation
    private var outputAllocation: Allocation
    private var tempAllocation: Allocation
    private var preProcessedAllocation: Allocation
    private var processingHandler: Handler
    private var processingTask: ProcessingTask

    init {
        val yuvTypeBuilder = Type.Builder(
            rs, Element.createPixel(
                rs,
                Element.DataType.UNSIGNED_8, Element.DataKind.PIXEL_YUV
            )
        )
        yuvTypeBuilder.setX(dimensions.width)
        yuvTypeBuilder.setY(dimensions.height)
        yuvTypeBuilder.setYuvFormat(ImageFormat.YUV_420_888)
        inputAllocation = Allocation.createTyped(
            rs, yuvTypeBuilder.create(),
            Allocation.USAGE_IO_INPUT or Allocation.USAGE_SCRIPT
        )

        preProcessedAllocation = Allocation.createTyped(
            rs, yuvTypeBuilder.create(),
            Allocation.USAGE_SCRIPT
        )

        tempAllocation = Allocation.createTyped(
            rs, yuvTypeBuilder.create(),
            Allocation.USAGE_SCRIPT
        )

        val rgbTypeBuilder = Type.Builder(rs, Element.RGBA_8888(rs))
        rgbTypeBuilder.setX(dimensions.width)
        rgbTypeBuilder.setY(dimensions.height)
        outputAllocation = Allocation.createTyped(
            rs, rgbTypeBuilder.create(),
            Allocation.USAGE_IO_OUTPUT or Allocation.USAGE_SCRIPT
        )


        choosePreProcessor(preProcessorChoice)
        chooseProcessor(processorChoice)

        val processingThread = HandlerThread("ProcessingCoordinator")
        processingThread.start()
        processingHandler = Handler(processingThread.looper)

        processingTask = ProcessingTask(
            processor,
            preProcessor,
            processingHandler,
            inputAllocation,
            outputAllocation
        )

    }

    internal class ProcessingTask(
        private val processor: Processor,
        private val preProcessor: PreProcessor,
        private val mProcessingHandler: Handler,
        private var inputAllocation: Allocation,
        private var outputAllocation: Allocation
    ) : Runnable, Allocation.OnBufferAvailableListener {
        override fun run() {
            var pendingFrames: Int
            synchronized(this) {
                pendingFrames = mPendingFrames
                mPendingFrames = 0
                // Discard extra messages in case processing is slower than frame rate
                mProcessingHandler.removeCallbacks(this)
            }
            // Get to newest input
            for (i in 0 until pendingFrames) {
                inputAllocation.ioReceive()
            }

            preProcessor.run()
            processor.run()
            outputAllocation.ioSend()
        }

        private var mPendingFrames = 0
        override fun onBufferAvailable(a: Allocation?) {
            synchronized(this) {
                mPendingFrames++
                mProcessingHandler.post(this)
            }
        }

        init {
            inputAllocation.setOnBufferAvailableListener(this)
        }

    }

    fun setOutputSurface(output: Surface?) {
        outputAllocation.surface = output
    }

    fun getInputSurface(): Surface {
        return inputAllocation.surface
    }

    fun chooseProcessor(processorChoice: Int) {
        when (processorChoice) {
            0 -> processor =
                SobelProcessor(rs, dimensions, preProcessedAllocation, outputAllocation)
        }
    }

    fun choosePreProcessor(preProcessorChoice: Int) {
        when (preProcessorChoice) {
            0 -> preProcessor = GaussianProcessor(
                rs,
                dimensions,
                inputAllocation,
                preProcessedAllocation,
                tempAllocation,
                10.0,
                5
            )
        }
    }

    fun closeAllocations() {
        inputAllocation.destroy()
        outputAllocation.destroy()
        tempAllocation.destroy()
    }
}