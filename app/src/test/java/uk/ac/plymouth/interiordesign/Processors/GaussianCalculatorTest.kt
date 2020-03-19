package uk.ac.plymouth.interiordesign.Processors

import org.junit.Test

import org.junit.Assert.*

class GaussianCalculatorTest {

    @Test
    fun createGaussianKernel() {
        val gaussianCalculator = GaussianCalculator(1.0, 5)
        val correctKernel = floatArrayOf(0.06136f, 0.24477f, 0.38774f, 0.24477f, 0.06136f)
        gaussianCalculator.createGaussianKernel()
        for (i in gaussianCalculator.kernel.indices) {
            kotlin.test.assertEquals(correctKernel[i], gaussianCalculator.kernel[i])
        }
    }
}