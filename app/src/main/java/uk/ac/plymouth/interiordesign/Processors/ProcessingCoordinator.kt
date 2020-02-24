package uk.ac.plymouth.interiordesign.Processors

import android.renderscript.RenderScript
import android.util.Size
import android.view.Surface

class ProcessingCoordinator(preProcessorChoice : Int, processorChoice: Int, rs : RenderScript, dimensions : Size) {
    private lateinit var preProcessor: PreProcessor
    private lateinit var processor: Processor

    init {
        when (preProcessorChoice) {
            0 -> preProcessor = GaussianProcessor(rs, dimensions)
        }

        when (processorChoice) {
            0 -> processor = SobelProcessor(rs, dimensions)
        }

        preProcessor.setOutputSurface(processor.getInputSurface())
    }

    fun setOutputSurface(output : Surface?) {
        processor.setOutputSurface(output)
    }

    fun getInputSurface() : Surface {
        return preProcessor.getInputSurface()
    }
}