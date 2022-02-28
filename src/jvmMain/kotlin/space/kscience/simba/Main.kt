import space.kscience.simba.CellEnvironmentState
import space.kscience.simba.CellState
import space.kscience.simba.GameOfLife
import kotlin.random.Random

fun nextStep(state: CellState, environmentState: CellEnvironmentState): CellState {
    val aliveNeighbours = environmentState.neighbours.count { it.isAlive() }
    if (state.isAlive) {
        if (aliveNeighbours != 2 && aliveNeighbours != 3) {
            return CellState(false)
        }
    } else if (aliveNeighbours == 3) {
        return CellState(true)
    }

    return state
}

fun main() {
    val random = Random(0)
    val game = GameOfLife(10, 10, ::nextStep) { _, _ -> CellState(random.nextBoolean()) }
    println(game.toString())

    for (i in 0 until 10) {
        game.iterate()
        println(game.toString())
    }
}