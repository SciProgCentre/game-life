package space.kscience.simba.state

import space.kscience.simba.utils.Vector
import space.kscience.simba.utils.compareTo

val gameOfLifeNeighbours: Set<Vector> = setOf(
    intArrayOf(-1, -1), intArrayOf(-1, 0), intArrayOf(-1, 1),
    intArrayOf(0, -1), intArrayOf(0, 1),
    intArrayOf(1, -1), intArrayOf(1, 0), intArrayOf(1, 1)
)

@kotlinx.serialization.Serializable
data class ActorGameOfLifeState(val i: Int, val j: Int, val isAlive: Boolean) : ObjectState<ActorGameOfLifeState, EnvironmentState>, Comparable<ActorGameOfLifeState> {
    override suspend fun iterate(neighbours: List<ActorGameOfLifeState>, env: EnvironmentState?): ActorGameOfLifeState {
        val aliveNeighbours = neighbours.count { it.isAlive }
        if (this.isAlive) {
            if (aliveNeighbours != 2 && aliveNeighbours != 3) {
                return ActorGameOfLifeState(this.i, this.j, false)
            }
        } else if (aliveNeighbours == 3) {
            return ActorGameOfLifeState(this.i, this.j, true)
        }

        return this
    }

    override fun isReadyForIteration(neighbours: List<ActorGameOfLifeState>, env: EnvironmentState?, expectedCount: Int): Boolean {
        return neighbours.size == expectedCount
    }

    override fun compareTo(other: ActorGameOfLifeState): Int {
        return intArrayOf(i, j).compareTo(intArrayOf(other.i, other.j))
    }

    override fun toString(): String {
        return "($i, $j) = $isAlive"
    }
}

fun classicState(i: Int, j: Int, state: Boolean) = ActorGameOfLifeState(i, j, state)

fun actorsToString(field: List<ActorGameOfLifeState>): String {
    val builder = StringBuilder()
    val n = field.maxOf { it.i } + 1
    val m = field.maxOf { it.j } + 1
    val sortedField = field.sorted()

    for (i in 0 until n) {
        for (j in 0 until m) {
            builder.append(if (sortedField[i * n + j].isAlive) "X" else "O")
        }
        builder.append("\n")
    }
    builder.append("\n")
    return builder.toString()
}
