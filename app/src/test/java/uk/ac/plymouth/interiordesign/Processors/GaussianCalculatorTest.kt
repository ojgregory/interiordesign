package uk.ac.plymouth.interiordesign.Processors

import org.junit.Test

import org.junit.Assert.*

class GaussianCalculatorTest {

    @Test
    fun createGaussianKernel() {
        val gaussianCalculator = GaussianCalculator(1.0, 5)
        val correctKernel = doubleArrayOf(0.06136, 0.24477, 0.38774, 0.24477, 0.06136)
        gaussianCalculator.createGaussianKernel()
        for (i in 0 until 5) {
            kotlin.test.assertEquals(correctKernel[i], gaussianCalculator.kernel[i])
        }
    }
}