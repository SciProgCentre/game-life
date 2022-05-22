package space.kscience.simba

val gameOfLifeNeighbours: Set<Vector> = setOf(
    intArrayOf(-1, -1), intArrayOf(-1, 0), intArrayOf(-1, 1),
    intArrayOf(0, -1), intArrayOf(0, 1),
    intArrayOf(1, -1), intArrayOf(1, 0), intArrayOf(1, 1)
)

@kotlinx.serialization.Serializable
data class ActorGameOfLifeState(val isAlive: Boolean) : ObjectState

data class ActorGameOfLifeEnv(val neighbours: MutableList<ActorGameOfLifeCell>) : EnvironmentState {}

@kotlinx.serialization.Serializable
data class ActorGameOfLifeCell(
    val i: Int, val j: Int, private val state: ActorGameOfLifeState
) : Cell<ActorGameOfLifeCell, ActorGameOfLifeState, ActorGameOfLifeEnv>() {
    @kotlinx.serialization.Transient
    private val environmentState = ActorGameOfLifeEnv(mutableListOf())

    override fun isReadyForIteration(expectedCount: Int): Boolean {
        return environmentState.neighbours.size == expectedCount
    }

    override fun addNeighboursState(cell: ActorGameOfLifeCell) {
        environmentState.neighbours.add(cell)
    }

    override fun iterate(convert: (ActorGameOfLifeState, ActorGameOfLifeEnv) -> ActorGameOfLifeState): ActorGameOfLifeCell {
        return ActorGameOfLifeCell(i, j, convert(state, environmentState))
    }

    fun isAlive(): Boolean {
        return state.isAlive
    }

    override fun compareTo(other: ActorGameOfLifeCell): Int {
        i.compareTo(other.i).let { if (it != 0) return it }
        j.compareTo(other.j).let { if (it != 0) return it }
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ActorGameOfLifeCell

        if (i != other.i) return false
        if (j != other.j) return false

        return true
    }

    override fun hashCode(): Int {
        var result = i
        result = 31 * result + j
        return result
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

    for (i in 0 until n) {
        for (j in 0 until m) {
            builder.append(if (field[i * n + j].isAlive()) "X" else "O")
        }
        builder.append("\n")
    }
    builder.append("\n")
    return builder.toString()
}
