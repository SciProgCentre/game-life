import space.kscience.simba.CellState
import space.kscience.simba.GameOfLife
import kotlin.random.Random

fun nextStep(state: CellState): Boolean {
    val aliveNeighbours = state.neighbours.count { it.isAlive() }
    if (state.isAlive) {
        if (aliveNeighbours != 2 && aliveNeighbours != 3) {
            return false
        }
    } else if (aliveNeighbours == 3) {
        return true
    }
    return false
}

fun main() {
    val random = Random(0)
    val game = GameOfLife(10, 10, ::nextStep) { _, _ -> random.nextBoolean() }
    println(game.toString())

    for (i in 0 until 10) {
        game.iterate()
        println(game.toString())
    }
}