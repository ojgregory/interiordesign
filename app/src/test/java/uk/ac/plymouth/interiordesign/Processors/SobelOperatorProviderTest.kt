package uk.ac.plymouth.interiordesign.Processors

import org.junit.Test

import org.junit.Assert.*

class SobelOperatorProviderTest {
    val sobelOperatorX = arrayOf(3, 0, -3, 10, 0, -10, 3, 0, -3)
    val sobelOperatorY = arrayOf(3, 10, 3, 0, 0, 0, -3, -10, -3)
    val scharrOperatorX = arrayOf(47, 0, -47, 162, 0, -162, 47, 0, -47)
    val scharrOperatorY = arrayOf(47, 162, 47, 0, 0, 0, -47, -162, -47)

    @Test
    fun setSelectedOperator() {
        val sobelOperatorProvider = SobelOperatorProvider()
        var operatorX = sobelOperatorProvider.getOperatorX()
        var operatorY = sobelOperatorProvider.getOperatorY()

        for (i in operatorX.indices) {
            kotlin.test.assertEquals(sobelOperatorX[i], operatorX[i])
        }

        for (i in operatorY.indices) {
            kotlin.test.assertEquals(sobelOperatorY[i], operatorY[i])
        }

        sobelOperatorProvider.selectedOperator = 1
        operatorX = sobelOperatorProvider.getOperatorX()
        operatorY = sobelOperatorProvider.getOperatorY()

        for (i in operatorX.indices) {
            kotlin.test.assertEquals(scharrOperatorX[i], operatorX[i])
        }

        for (i in operatorY.indices) {
            kotlin.test.assertEquals(scharrOperatorY[i], operatorY[i])
        }
    }

    @Test
    fun getOperatorX() {
        val sobelOperatorProvider = SobelOperatorProvider()
        var operatorX = sobelOperatorProvider.getOperatorX()

        for (i in operatorX.indices) {
            kotlin.test.assertEquals(sobelOperatorX[i], operatorX[i])
        }
    }

    @Test
    fun getOperatorY() {
        val sobelOperatorProvider = SobelOperatorProvider()
        var operatorY = sobelOperatorProvider.getOperatorY()

        for (i in operatorY.indices) {
            kotlin.test.assertEquals(sobelOperatorY[i], operatorY[i])
        }
    }
}