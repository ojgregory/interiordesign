package uk.ac.plymouth.interiordesign.Fillers

import android.renderscript.*
import android.util.Size
import uk.ac.plymouth.interiordesign.Room.Colour
import uk.ac.plymouth.interiordesign.ScriptC_dummy
import uk.ac.plymouth.interiordesign.ScriptC_floodfill

// Parallel implementation of flood fill, uses frontier switching
class FloodFillParallel(
    rs: RenderScript,
    override var mInputAllocation: Allocation,
    override var mOutputAllocation: Allocation,
    override var mOriginalAllocation: Allocation,
    private var dimensions: Size,
    override var colour: Colour
) : Filler {
    private val parallelScript = ScriptC_floodfill(rs)
    private val dummyScript = ScriptC_dummy(rs)

    //private val isProcessed = Allocation.createTyped(rs, Type.createXY(rs, Element.U8(rs), dimensions.width, dimensions.height))
    override var x: Int = 0
        get() = field
        set(value) {
            field = value
        }
    override var y: Int = 0
        get() = field
        set(value) {
            field = value
        }


    override fun run() {
        parallelScript._imageH = dimensions.height
        parallelScript._imageW = dimensions.width
        dummyScript._gCurrentFrame = mOriginalAllocation
        dummyScript.forEach_convertYToRGB_colour(mOutputAllocation)
        // Don't run if no x and y is selected
        if (x != 0 && y != 0) {
            parallelScript._input = mInputAllocation
            parallelScript._output = mOutputAllocation
            parallelScript._colour = Short4(
                colour.r.toShort(), colour.g.toShort(),
                colour.b.toShort(), colour.a.toShort()
            )
            parallelScript.invoke_parallel_implementation(x, y, 0)
        }
    }
}