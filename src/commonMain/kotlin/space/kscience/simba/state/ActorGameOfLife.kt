package space.kscience.simba.state

import space.kscience.simba.utils.Vector

val gameOfLifeNeighbours: Set<Vector> = setOf(
    intArrayOf(-1, -1), intArrayOf(-1, 0), intArrayOf(-1, 1),
    intArrayOf(0, -1), intArrayOf(0, 1),
    intArrayOf(1, -1), intArrayOf(1, 0), intArrayOf(1, 1)
)

@kotlinx.serialization.Serializable
data class ActorGameOfLifeState(val isAlive: Boolean) : ObjectState

@kotlinx.serialization.Serializable
data class ActorGameOfLifeEnv(val neighbours: MutableList<ActorGameOfLifeCell>) : EnvironmentState {}

@kotlinx.serialization.Serializable
data class ActorGameOfLifeCell(
    val i: Int, val j: Int,
    override val state: ActorGameOfLifeState,
    override val environmentState: ActorGameOfLifeEnv = ActorGameOfLifeEnv(mutableListOf())
) : Cell<ActorGameOfLifeCell, ActorGameOfLifeState, ActorGameOfLifeEnv>() {
    override val vectorId: Vector = intArrayOf(i, j)

    override fun isReadyForIteration(expectedCount: Int): Boolean {
        return environmentState.neighbours.size == expectedCount
    }

    override fun addNeighboursState(cell: ActorGameOfLifeCell) {
        environmentState.neighbours.add(cell)
    }

    override fun iterate(
        convertState: (ActorGameOfLifeState, ActorGameOfLifeEnv) -> ActorGameOfLifeState,
        convertEnv: (ActorGameOfLifeState, ActorGameOfLifeEnv) -> ActorGameOfLifeEnv
    ): ActorGameOfLifeCell {
        return ActorGameOfLifeCell(i, j, convertState(state, environmentState), convertEnv(state, environmentState))
    }

    fun isAlive(): Boolean {
        return state.isAlive
    }

    override fun toString(): String {
        return "($i, $j) = ${state.isAlive}"
    }
}

fun classicCell(i: Int, j: Int, state: Boolean) = ActorGameOfLifeCell(i, j, ActorGameOfLifeState(state))

fun actorNextStep(state: ActorGameOfLifeState, environmentState: ActorGameOfLifeEnv): ActorGameOfLifeState {
    val aliveNeighbours = environmentState.neighbours.count { it.isAlive() }
    if (state.isAlive) {
        if (aliveNeighbours != 2 && aliveNeighbours != 3) {
            return ActorGameOfLifeState(false)
        }
    } else if (aliveNeighbours == 3) {
        return ActorGameOfLifeState(true)
    }

    return state
}

fun actorsToString(field: List<ActorGameOfLifeCell>): String {
    val builder = StringBuilder()
    val n = field.maxOf { it.i } + 1
    val m = field.maxOf { it.j } + 1
    val sortedField = field.sorted()

    for (i in 0 until n) {
        for (j in 0 until m) {
            builder.append(if (sortedField[i * n + j].isAlive()) "X" else "O")
        }
        builder.append("\n")
    }
    builder.append("\n")
    return builder.toString()
}
