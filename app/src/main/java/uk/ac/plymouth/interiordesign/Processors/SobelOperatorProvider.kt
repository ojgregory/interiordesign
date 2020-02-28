package uk.ac.plymouth.interiordesign.Processors

class SobelOperatorProvider {
    val maskSize = 3
    val sobelOperatorX = arrayOf(3, 0, -3, 10, 0, -10, 3, 0, -3)
    val sobelOperatorY = arrayOf(3, 10, 3, 0, 0, 0, -3, -10, -3)
    val scharrOperatorX = arrayOf(47, 0, -47, 162, 0, -162, 47, 0, -47)
    val scharrOperatorY = arrayOf(47, 162, 47, 0, 0, 0, -47, -162, -47)
    var selectedOperator = 0
        set(value) {
            if (value in 0..1)
                field = value
        }

    fun getOperatorX() : Array<Int> {
        when (selectedOperator) {
            0 -> return sobelOperatorX
            1 -> return scharrOperatorX
        }
        return arrayOf(maskSize)
    }

    fun getOperatorY() : Array<Int> {
        when (selectedOperator) {
            0 -> return sobelOperatorY
            1 -> return scharrOperatorY
        }
        return arrayOf(maskSize)
    }


}