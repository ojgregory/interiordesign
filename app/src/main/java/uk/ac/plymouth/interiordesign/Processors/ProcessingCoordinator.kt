package uk.ac.plymouth.interiordesign.Processors

import android.graphics.ImageFormat
import android.os.Handler
import android.os.HandlerThread
import android.renderscript.*
import android.util.Size
import android.view.Surface
import uk.ac.plymouth.interiordesign.Fillers.DummyFiller
import uk.ac.plymouth.interiordesign.Fillers.Filler
import uk.ac.plymouth.interiordesign.Fillers.FloodFillParallel
import uk.ac.plymouth.interiordesign.Fillers.FloodFillSerial
import uk.ac.plymouth.interiordesign.Room.Colour

class ProcessingCoordinator(
    preProcessorChoice: Int,
    processorChoice: Int,
    fillerChoice : Int,
    private val rs: RenderScript,
    private val dimensions: Size
) {
    private lateinit var preProcessor: PreProcessor
    private lateinit var processor: Processor
    private lateinit var filler: Filler
    private var inputAllocation: Allocation
    private var outputAllocation: Allocation
    private var tempAllocation: Allocation
    private var preProcessedAllocation: Allocation
    private var processingHandler: Handler
    private var processingTask: ProcessingTask
    private lateinit var colour: Colour

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
        chooseFiller(fillerChoice)

        val processingThread = HandlerThread("ProcessingCoordinator")
        processingThread.start()
        processingHandler = Handler(processingThread.looper)

        processingTask = ProcessingTask(
            processor,
            preProcessor,
            filler,
            processingHandler,
            inputAllocation,
            outputAllocation
        )
    }

    internal class ProcessingTask(
        processor: Processor,
        preProcessor: PreProcessor,
        filler: Filler,
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
            filler.run()
            outputAllocation.ioSend()
        }

        var processor = processor
        var preProcessor = preProcessor
        var filler = filler

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
            0 -> processor = DummyProcessor(rs, preProcessedAllocation, tempAllocation)
            1 -> if (::processor.isInitialized && processor is SobelProcessor)
                    (processor as SobelProcessor).changeOperators(0)
                 else {
                  processor =
                        SobelProcessor(rs, dimensions, preProcessedAllocation, tempAllocation)
                    (processor as SobelProcessor).changeOperators(0)
                 }
            2 -> {
                if (::processor.isInitialized && processor is SobelProcessor)
                    (processor as SobelProcessor).changeOperators(1)
                else {
                    processor =
                        SobelProcessor(rs, dimensions, preProcessedAllocation, tempAllocation)
                    (processor as SobelProcessor).changeOperators(1)
                }
            }
            3 -> {
                if (::processor.isInitialized && processor !is RobertsCrossProcessor)
                    processor =
                        RobertsCrossProcessor(rs, dimensions, preProcessedAllocation, tempAllocation)
            }
            4 -> {
                if (::processor.isInitialized && processor !is PrewittProcessor)
                    processor =
                        PrewittProcessor(rs, dimensions, preProcessedAllocation, tempAllocation)
            }
            5 -> {
                if (::processor.isInitialized && processor !is CannyProcessor)
                    processor =
                        CannyProcessor(rs, dimensions, preProcessedAllocation, tempAllocation, 21, 10)
            }
        }
        processingTask?.processor = processor
    }

    fun chooseFiller(fillerChoice: Int) {
        var x = 0
        var y = 0
        if (::filler.isInitialized) {
            x = filler.x
            y = filler.y
        }
        if (!::colour.isInitialized)
            colour = Colour(255, 255, 255, 255, "White")
        when (fillerChoice) {
            0 -> filler = DummyFiller(rs, tempAllocation, outputAllocation, inputAllocation, colour)
            1 -> filler = FloodFillSerial(rs, tempAllocation, outputAllocation, inputAllocation, dimensions, colour)
            2 -> filler = FloodFillParallel(rs, tempAllocation, outputAllocation, inputAllocation, dimensions, colour)
        }

        filler.x = x
        filler.y = y

        if (processingTask != null)
            processingTask.filler = filler
    }

    fun setFillerXandY(x : Int, y: Int) {
        filler.x = x
        filler.y = y
    }

    fun choosePreProcessor(preProcessorChoice: Int) {
        when (preProcessorChoice) {
            0 -> preProcessor = DummyPreprocessor(
                rs,
                inputAllocation,
                preProcessedAllocation,
                tempAllocation
            )
            1 -> preProcessor = GaussianProcessor(
                rs,
                dimensions,
                inputAllocation,
                preProcessedAllocation,
                tempAllocation,
                10.0,
                5
            )
        }
        if (processingTask != null)
            processingTask.preProcessor = preProcessor
    }

    fun setGaussianMaskSize(size : Int) {
        if (preProcessor is GaussianProcessor) {
            (preProcessor as GaussianProcessor).maskSize = size
            (preProcessor as GaussianProcessor).recalculateGaussianKernel()
        }
    }

    fun setGaussianSigma(sigma : Double) {
        if (preProcessor is GaussianProcessor) {
            (preProcessor as GaussianProcessor).sigma = sigma
            (preProcessor as GaussianProcessor).recalculateGaussianKernel()
        }
    }

    fun closeAllocations() {
        inputAllocation.destroy()
        outputAllocation.destroy()
        tempAllocation.destroy()
    }

    fun setColour(c: Colour) {
        this.colour = c
        filler.colour = colour
    }
}