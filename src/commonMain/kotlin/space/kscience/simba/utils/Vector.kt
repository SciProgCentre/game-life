package space.kscience.simba.utils

import kotlin.math.max

typealias Vector = IntArray

fun IntArray.product(): Int {
    return this.reduce(Int::times)
}

fun Int.toVector(dimensions: Vector): Vector {
    return indexFromOffset(this, stridesFromShape(dimensions), dimensions.size)
}

fun Vector.toIndex(dimensions: Vector): Int {
    val strides = stridesFromShape(dimensions)
    return this.mapIndexed { i, value -> value * strides[i] }.sum()
}

/**
 * This [Strides] implementation follows the last dimension first convention
 * For more information: https://numpy.org/doc/stable/reference/generated/numpy.ndarray.strides.html
 */
private fun stridesFromShape(shape: IntArray): IntArray {
    val nDim = shape.size
    val res = IntArray(nDim)
    if (nDim == 0)
        return res

    var current = nDim - 1
    res[current] = 1

    while (current > 0) {
        res[current - 1] = max(1, shape[current]) * res[current]
        current--
    }
    return res
}

private fun indexFromOffset(offset: Int, strides: IntArray, nDim: Int): IntArray {
    val res = IntArray(nDim)
    var current = offset
    var strideIndex = 0

    while (strideIndex < nDim) {
        res[strideIndex] = (current / strides[strideIndex])
        current %= strides[strideIndex]
        strideIndex++
    }
    return res
}
