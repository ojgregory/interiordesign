package uk.ac.plymouth.interiordesign.Fillers

import android.renderscript.Allocation
import android.renderscript.RenderScript
import android.util.Size
import androidx.annotation.Dimension
import uk.ac.plymouth.interiordesign.ScriptC_floodfill

class FloodFillSerial(
    rs : RenderScript,
    override var mInputAllocation: Allocation,
    override var mOutputAllocation: Allocation,
    private var dimensions: Size
) : Filler  {
    private val serialScript = ScriptC_floodfill(rs)
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
        serialScript._imageH = dimensions.height
        serialScript._imageW = dimensions.width
        serialScript.invoke_serial_implementation(mInputAllocation, mOutputAllocation, x, y, 0)
    }
}