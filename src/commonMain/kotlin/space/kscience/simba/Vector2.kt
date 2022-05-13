package space.kscience.simba

import kotlin.math.sqrt
import kotlin.random.Random

typealias Vector2 = Pair<Double, Double>

val zero: Vector2 = 0.0 to 0.0

operator fun Vector2.plus(other: Vector2): Vector2 {
    return this.first + other.first to this.second + other.second
}

operator fun Vector2.minus(other: Vector2): Vector2 {
    return this.first - other.first to this.second - other.second
}

operator fun Vector2.times(other: Double): Vector2 {
    return this.first * other to this.second * other
}

operator fun Vector2.div(other: Double): Vector2 {
    return this.first / other to this.second / other
}

operator fun Double.times(vector: Vector2): Vector2 = vector * this

fun Vector2.length(): Double = sqrt(this.first * this.first + this.second * this.second)
fun Vector2.normalized(): Vector2 = this / length()
fun Random.randomVector() = Vector2(this.nextDouble(), this.nextDouble())