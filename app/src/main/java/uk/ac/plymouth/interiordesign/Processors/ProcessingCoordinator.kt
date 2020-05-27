package uk.ac.plymouth.interiordesign.Processors

import android.graphics.ImageFormat
import android.os.Handler
import android.os.HandlerThread
import android.renderscript.*
import android.util.Size
import android.view.Surface
import androidx.preference.PreferenceManager
import uk.ac.plymouth.interiordesign.Fillers.*
import uk.ac.plymouth.interiordesign.Room.Colour
import java.lang.NullPointerException

// Organise the edge detection, blurring and filling algorithms
class ProcessingCoordinator(
    preProcessorChoice: Int,
    processorChoice: Int,
    fillerChoice: Int,
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
    val processingThread = HandlerThread("ProcessingCoordinator")
    val prefs = PreferenceManager.getDefaultSharedPreferences(rs.applicationContext)
    val showFacade = prefs.getBoolean("facade_check", true)

    init {
        // Initialise shared allocations
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


        // Initialise preProcessor, processor, and filler based on choices
        choosePreProcessor(preProcessorChoice)
        chooseProcessor(processorChoice)
        chooseFiller(fillerChoice)

        processingThread.start()
        processingHandler = Handler(processingThread.looper)

        // Launch processing task
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
            if (!stopped) {
                var pendingFrames: Int
                synchronized(this) {
                    pendingFrames = mPendingFrames
                    mPendingFrames = 0
                    // Discard extra messages in case processing is slower than frame rate
                    mProcessingHandler.removeCallbacks(this)
                }

                //Only crashes if the allocations have been cleared which means that
                // the task should stop, for example activity change
                try {
                    // Get to newest input
                    for (i in 0 until pendingFrames) {
                        inputAllocation.ioReceive()
                    }

                    // Run the algorithms in correct order
                    // If any are dummy then they apply no processing
                    preProcessor.run()
                    processor.run()
                    filler.run()
                    outputAllocation.ioSend()
                } catch (e: RSIllegalArgumentException) {
                    e.printStackTrace()
                    stopped = true
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                    stopped = true
                }
            }
        }

        fun stop() {
            stopped = true
        }

        var processor = processor
        var preProcessor = preProcessor
        var filler = filler
        var stopped: Boolean = false

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

    private fun chooseProcessor(processorChoice: Int) {
        when (processorChoice) {
            0 -> processor = DummyProcessor(rs, preProcessedAllocation, tempAllocation)
            1 -> {
                // Sobel and Scharr share class so change operator to sobel(0)
                if (::processor.isInitialized && processor is SobelProcessor)
                    (processor as SobelProcessor).changeOperators(0)
                else {
                    processor =
                        SobelProcessor(rs, dimensions, preProcessedAllocation, tempAllocation)
                    (processor as SobelProcessor).changeOperators(0)
                }
            }
            2 -> {
                // Sobel and Scharr share class so change operator to scharr(1)
                if (::processor.isInitialized && processor is SobelProcessor)
                    (processor as SobelProcessor).changeOperators(1)
                else {
                    processor =
                        SobelProcessor(rs, dimensions, preProcessedAllocation, tempAllocation)
                    (processor as SobelProcessor).changeOperators(1)
                }
            }
            3 -> {
                processor =
                    RobertsCrossProcessor(
                        rs,
                        dimensions,
                        preProcessedAllocation,
                        tempAllocation
                    )
            }
            4 -> {
                processor =
                    PrewittProcessor(rs, dimensions, preProcessedAllocation, tempAllocation)
            }
            5 -> {
                processor =
                    CannyProcessor(
                        rs,
                        dimensions,
                        preProcessedAllocation,
                        tempAllocation,
                        21,
                        10
                    )
            }
        }

        // Processing task may not be initialised yet
        processingTask?.processor = processor
    }

    fun chooseFiller(fillerChoice: Int) {
        var x = 0
        var y = 0
        if (::filler.isInitialized) {
            x = filler.x
            y = filler.y
        }
        // Set default colour if none present
        if (!::colour.isInitialized)
            colour = Colour(255, 255, 255, 255, "White")
        when (fillerChoice) {
            0 -> if (showFacade)
                filler = DummyFiller(rs, tempAllocation, outputAllocation, inputAllocation, colour, showColour = true)
            else
                filler = DummyFiller(rs, tempAllocation, outputAllocation, tempAllocation, colour, showColour = false)
            1 -> if (showFacade)
                filler = FloodFillSerial(
                    rs,
                    tempAllocation,
                    outputAllocation,
                    inputAllocation,
                    dimensions,
                    colour,
                    showColour = true
                )
            else
                filler = FloodFillSerial(
                    rs,
                    tempAllocation,
                    outputAllocation,
                    tempAllocation,
                    dimensions,
                    colour,
                    showColour = false
                )
            2 -> if (showFacade)
                filler = FloodFillSerialAlt(
                    rs,
                    tempAllocation,
                    outputAllocation,
                    inputAllocation,
                    dimensions,
                    colour,
                    showColour = true
                )
            else
                filler = FloodFillSerialAlt(
                    rs,
                    tempAllocation,
                    outputAllocation,
                    tempAllocation,
                    dimensions,
                    colour,
                    showColour = false
                )
            3 -> if (showFacade)
                filler = FloodFillParallel(
                    rs,
                    tempAllocation,
                    outputAllocation,
                    inputAllocation,
                    dimensions,
                    colour,
                    showColour = true
                )
            else
                filler = FloodFillParallel(
                    rs,
                    tempAllocation,
                    outputAllocation,
                    tempAllocation,
                    dimensions,
                    colour,
                    showColour = false
                )
        }

        filler.x = x
        filler.y = y

        // Processing task may not be initialised yet
        processingTask?.filler = filler
    }

    fun setFillerXandY(x: Int, y: Int) {
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
        // Processing task may not be initialised yet
        processingTask?.preProcessor = preProcessor
    }

    fun setGaussianMaskSize(size: Int) {
        if (preProcessor is GaussianProcessor) {
            (preProcessor as GaussianProcessor).maskSize = size
            (preProcessor as GaussianProcessor).recalculateGaussianKernel()
        }
    }

    fun setGaussianSigma(sigma: Double) {
        if (preProcessor is GaussianProcessor) {
            (preProcessor as GaussianProcessor).sigma = sigma
            (preProcessor as GaussianProcessor).recalculateGaussianKernel()
        }
    }

    fun closeAllocationsAndStop() {
        inputAllocation.destroy()
        outputAllocation.destroy()
        tempAllocation.destroy()
        processingHandler.removeCallbacks(processingTask)
        processingTask.stop()
    }

    fun setColour(c: Colour) {
        this.colour = c
        filler.colour = colour
    }
}