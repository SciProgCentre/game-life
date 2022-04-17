package space.kscience.simba

@kotlinx.serialization.Serializable
data class ActorCellState(var isAlive: Boolean): ObjectState

@kotlinx.serialization.Serializable
data class ActorCellEnvironmentState(val neighbours: MutableList<ActorClassicCell>) : EnvironmentState {}

@kotlinx.serialization.Serializable
class ActorClassicCell(
    val i: Int, val j: Int, private var state: ActorCellState
): Cell<ActorCellEnvironmentState, ActorCellState>(), Comparable<ActorClassicCell> {
    private val environmentState = ActorCellEnvironmentState(mutableListOf())
    private var oldState = state

    override fun iterate(convert: (ActorCellState, ActorCellEnvironmentState) -> ActorCellState) {
        state = convert(state, environmentState)
    }

    override fun endIteration() {
        environmentState.neighbours.clear()
        oldState = state.copy()
    }

    fun addNeighboursState(cell: ActorClassicCell) {
        environmentState.neighbours.add(cell)
    }

    fun isAlive(): Boolean {
        return oldState.isAlive
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