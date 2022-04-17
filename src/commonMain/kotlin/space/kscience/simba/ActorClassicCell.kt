package space.kscience.simba

@kotlinx.serialization.Serializable
data class ActorCellState(val isAlive: Boolean): ObjectState

data class ActorCellEnvironmentState(val neighbours: MutableList<ActorClassicCell>) : EnvironmentState {}

@kotlinx.serialization.Serializable
data class ActorClassicCell(
    val i: Int, val j: Int, private val state: ActorCellState
): Cell<ActorCellEnvironmentState, ActorCellState>(), Comparable<ActorClassicCell> {
    @kotlinx.serialization.Transient
    private val environmentState = ActorCellEnvironmentState(mutableListOf())

    override fun iterate(convert: (ActorCellState, ActorCellEnvironmentState) -> ActorCellState): ActorClassicCell {
        return ActorClassicCell(i, j, convert(state, environmentState))
    }

    fun addNeighboursState(cell: ActorClassicCell) {
        environmentState.neighbours.add(cell)
    }

    fun isAlive(): Boolean {
        return state.isAlive
    }

    fun isReadyForIteration(expectedCount: Int): Boolean {
        return environmentState.neighbours.size == expectedCount
    }

    override fun compareTo(other: ActorClassicCell): Int {
        i.compareTo(other.i).let { if (it != 0) return it }
        j.compareTo(other.j).let { if (it != 0) return it }
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ActorClassicCell

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