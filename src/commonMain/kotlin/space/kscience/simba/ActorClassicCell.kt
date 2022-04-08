package space.kscience.simba

@kotlinx.serialization.Serializable
data class ActorCellState(var isAlive: Boolean): ObjectState

@kotlinx.serialization.Serializable
data class ActorCellEnvironmentState(val neighbours: MutableList<ActorClassicCell>) : EnvironmentState {}

@kotlinx.serialization.Serializable
class ActorClassicCell(val i: Int, val j: Int, private var state: ActorCellState): Cell<ActorCellEnvironmentState, ActorCellState>() {
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
}