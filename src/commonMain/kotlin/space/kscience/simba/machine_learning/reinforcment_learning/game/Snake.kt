package space.kscience.simba.machine_learning.reinforcment_learning.game

import space.kscience.simba.utils.*
import kotlin.math.floor
import kotlin.random.Random

class Snake(val width: Int, val height: Int, seed: Int = 0) {
    enum class Direction(val position: Vector2) {
        UP(Vector2(0.0, 1.0)), DOWN(Vector2(0.0, -1.0)), LEFT(Vector2(-1.0, 0.0)), RIGHT(Vector2(1.0, 0.0));

        fun getOpposite(): Direction {
            return when(this) {
                UP -> DOWN
                DOWN -> UP
                LEFT -> RIGHT
                RIGHT -> LEFT
            }
        }
    }

    private val rand = Random(seed)

    private var headPosition: Vector2 = zero
    private var baitPosition: Vector2 = zero
    private val body = mutableListOf<Vector2>() // all parts of snake without head
    private var ateBaitLastMove = false
    private var gameOver = false

    init {
        if (width <= 0) throw AssertionError("Width must be positive, but got width=${width}")
        if (height <= 0) throw AssertionError("Height must be positive, but got height=${height}")

        restart()
    }

    fun restart() {
        body.clear()
        ateBaitLastMove = false
        gameOver = false
        headPosition = randomVectorWithinBounds()
        generateNewBait()
    }

    fun move(direction: Direction) {
        if (gameOver) throw AssertionError("Game is over")

        body.add(0, headPosition)
        if (!ateBaitLastMove) body.removeLast() else ateBaitLastMove = false

        headPosition += direction.position
        if (body.isNotEmpty() && headPosition == body.last()) return // ignore move if snake moves backward
        if (isHeadCollidingWithWall() || isHeadCollidingWithBody()) gameOver = true
        if (headPosition == baitPosition) eatBait()
    }

    private fun isHeadCollidingWithWall(): Boolean {
        return headPosition.first < 0 || headPosition.first >= width ||
                headPosition.second < 0 || headPosition.second >= height
    }

    private fun isHeadCollidingWithBody(): Boolean {
        return body.isNotEmpty() && body.any { it == headPosition }
    }

    private fun eatBait() {
        ateBaitLastMove = true
        generateNewBait()
    }

    fun ateBait(): Boolean = ateBaitLastMove

    private fun generateNewBait() {
        do {
            baitPosition = randomVectorWithinBounds()
        } while (body.any { it == baitPosition } || baitPosition == headPosition)
    }

    private fun randomVectorWithinBounds(): Vector2 {
        return Vector2(floor(rand.nextDouble() * width), floor(rand.nextDouble() * height))
    }

    fun isGameOver() = gameOver

    fun getHeadPosition(): Vector2 = headPosition

    fun getBodyWithHead(): List<Vector2> = body + headPosition

    fun getBaitPosition(): Vector2? = baitPosition.takeUnless { ateBaitLastMove }
}
