package space.kscience.simba

data class CellState(var isAlive: Boolean, val neighbours: MutableList<Cell>) {}

class Cell(val i: Int, val j: Int, initState: Boolean) {
    private val state: CellState = CellState(initState, mutableListOf())
    private var oldState = initState

    fun iterate(convert: (CellState) -> Boolean) {
        state.isAlive = convert(state)
    }

    fun addNeighbour(cell: Cell) {
        state.neighbours.add(cell)
    }

    fun endIteration() {
        oldState = state.isAlive
    }

    fun isAlive(): Boolean {
        return oldState
    }
}