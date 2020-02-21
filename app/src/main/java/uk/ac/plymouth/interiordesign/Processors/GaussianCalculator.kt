package uk.ac.plymouth.interiordesign.Processors

import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

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
            var x = i - (mMaskSize / 2)
            kernel[i] = (constant * exp(-(x * x) / divisor)).toFloat()
            acc += kernel[i]
        }

        for (i in 0 until mMaskSize) {
            kernel[i] = (kernel[i] / acc).toFloat()
            kernel[i] = (Math.round(kernel[i] * 100000.0) / 100000.0).toFloat()
        }

    }




}
