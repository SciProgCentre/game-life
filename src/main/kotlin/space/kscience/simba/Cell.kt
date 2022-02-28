package space.kscience.simba

data class CellState(var isAlive: Boolean, val neighbours: MutableList<Cell>) {}

class Cell(val i: Int, val j: Int, initState: Boolean) {
    private val state: CellState = CellState(initState, mutableListOf())
    private var oldState = initState

    fun iterate() {
        val aliveNeighbours = state.neighbours.count { it.oldState }
        if (state.isAlive) {
            if (aliveNeighbours != 2 && aliveNeighbours != 3) {
                state.isAlive = false
            }
        } else if (aliveNeighbours == 3) {
            state.isAlive = true
        }
    }

    fun addNeighbour(cell: Cell) {
        state.neighbours.add(cell)
    }

    fun endIteration() {
        oldState = state.isAlive
    }

    fun isAlive(): Boolean {
        return state.isAlive
    }
}