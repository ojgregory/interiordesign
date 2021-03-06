package uk.ac.plymouth.interiordesign.Processors

import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

// Samples gaussian function to create kernel for blur
// Only creates maskSizex1 size kernels as that is the only
// type supported by Renderscript implementation
class GaussianCalculator(sigma : Double, maskSize: Int) {
    var mSigma : Double = sigma
        get() = field
        set(value) {
            field = value
            createGaussianKernel()
        }
    var mMaskSize : Int = maskSize
        get() = field
        set(value) {
            field = value
            kernel = FloatArray(value)
            createGaussianKernel()
        }
    var kernel = FloatArray(maskSize)


    fun createGaussianKernel() {
        val constant = 1 / sqrt(2 * Math.PI * mSigma.pow(2))
        val divisor = 2 * mSigma.pow(2)

        var acc = 0.0

        for  (i in 0 until mMaskSize) {
            val x = i - (mMaskSize / 2)
            kernel[i] = (constant * exp(-(x * x) / divisor)).toFloat()
            acc += kernel[i]
        }

        for (i in 0 until mMaskSize) {
            kernel[i] = (kernel[i] / acc).toFloat()
        }

    }




}
