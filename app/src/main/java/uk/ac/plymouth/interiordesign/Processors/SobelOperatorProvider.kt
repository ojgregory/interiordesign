package uk.ac.plymouth.interiordesign.Processors

class SobelOperatorProvider {
    val maskSize = 3
    val sobelOperatorX = intArrayOf(3, 0, -3, 10, 0, -10, 3, 0, -3)
    val sobelOperatorY = intArrayOf(3, 10, 3, 0, 0, 0, -3, -10, -3)
    val scharrOperatorX = intArrayOf(47, 0, -47, 162, 0, -162, 47, 0, -47)
    val scharrOperatorY = intArrayOf(47, 162, 47, 0, 0, 0, -47, -162, -47)
    var selectedOperator = 0
        set(value) {
            if (value in 0..1)
                field = value
        }

    fun getOperatorX() : IntArray {
        when (selectedOperator) {
            0 -> return sobelOperatorX
            1 -> return scharrOperatorX
        }
        return IntArray(maskSize * maskSize)
    }

    fun getOperatorY() : IntArray {
        when (selectedOperator) {
            0 -> return sobelOperatorY
            1 -> return scharrOperatorY
        }
        return IntArray(maskSize * maskSize)
    }


}