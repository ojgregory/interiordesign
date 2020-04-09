package uk.ac.plymouth.interiordesign.Fillers

import android.renderscript.Allocation
import android.renderscript.RenderScript
import android.util.Size
import uk.ac.plymouth.interiordesign.ScriptC_dummy
import uk.ac.plymouth.interiordesign.ScriptC_floodfill

class FloodFillParallel(
    rs : RenderScript,
    override var mInputAllocation: Allocation,
    override var mOutputAllocation: Allocation,
    private var dimensions: Size
) : Filler  {
    private val parallelScript = ScriptC_floodfill(rs)
    private val dummyScript = ScriptC_dummy(rs)
    override var x: Int = 5
        get() = field
        set(value) {
            field = value
        }
    override var y: Int = 5
        get() = field
        set(value) {
            field = value
        }


    override fun run() {
        parallelScript._imageH = dimensions.height
        parallelScript._imageW = dimensions.width
        dummyScript._gCurrentFrame = mInputAllocation
        dummyScript.forEach_convertYToRGB(mOutputAllocation)
        parallelScript._input = mInputAllocation
        parallelScript._output = mOutputAllocation
        println(x)
        println(y)
        parallelScript.invoke_parallel_implementation(x, y, 0)
    }
}