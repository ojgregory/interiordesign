package uk.ac.plymouth.interiordesign.Fillers

import android.renderscript.Allocation
import android.renderscript.RSIllegalArgumentException
import android.renderscript.RenderScript
import android.renderscript.Short4
import android.util.Size
import androidx.annotation.Dimension
import uk.ac.plymouth.interiordesign.Room.Colour
import uk.ac.plymouth.interiordesign.ScriptC_dummy
import uk.ac.plymouth.interiordesign.ScriptC_floodfill

// Standard BFS Flood Fill, no frontier switching
class FloodFillSerialAlt(
    var rs: RenderScript,
    override var mInputAllocation: Allocation,
    override var mOutputAllocation: Allocation,
    override var mOriginalAllocation: Allocation,
    private var dimensions: Size,
    override var colour: Colour
) : Filler {
    private val serialScript = ScriptC_floodfill(rs)
    private val dummyScript = ScriptC_dummy(rs)
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
        serialScript._imageH = dimensions.height
        serialScript._imageW = dimensions.width
        dummyScript._gCurrentFrame = mOriginalAllocation
        dummyScript.forEach_convertYToRGB_colour(mOutputAllocation)
        if (x != 0 && y != 0) {
            serialScript._colour = Short4(
                colour.r.toShort(), colour.g.toShort(),
                colour.b.toShort(), colour.a.toShort()
            )
            serialScript._output = mOutputAllocation
            serialScript._input = mInputAllocation
            serialScript.invoke_serial_implementation_while(x, y, 0)
        }
    }
}