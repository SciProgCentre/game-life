package space.kscience.simba

fun Double.clamp(min: Double, max: Double): Double {
    if (this > max) return max
    if (this < min) return min
    return this
}

fun Double.clampAndSwap(min: Double, max: Double): Double {
    if (this > max) return min
    if (this < min) return max
    return this
}