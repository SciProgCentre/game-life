import space.kscience.simba.GameOfLife
import kotlin.random.Random

fun main() {
    val random = Random(0)
    val game = GameOfLife(10, 10) { _, _ -> random.nextBoolean() }
    println(game.toString())

    for (i in 0 until 10) {
        game.iterate()
        println(game.toString())
    }
}