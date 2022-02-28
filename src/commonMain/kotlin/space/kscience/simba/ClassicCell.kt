package space.kscience.simba

data class CellState(var isAlive: Boolean): ObjectState

data class CellEnvironmentState(val neighbours: MutableList<ClassicCell>) : EnvironmentState {}

class ClassicCell(val i: Int, val j: Int, private var state: CellState): Cell<CellEnvironmentState, CellState>() {
    private val environmentState = CellEnvironmentState(mutableListOf())
    private var oldState = state

    override fun iterate(convert: (CellState, CellEnvironmentState) -> CellState) {
        state = convert(state, environmentState)
    }

    override fun endIteration() {
        oldState = state.copy()
    }

    fun addNeighbour(cell: ClassicCell) {
        environmentState.neighbours.add(cell)
    }

    fun isAlive(): Boolean {
        return oldState.isAlive
    }
}