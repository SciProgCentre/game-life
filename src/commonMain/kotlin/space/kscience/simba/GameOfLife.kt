package space.kscience.simba

fun classicNextStep(state: CellState, environmentState: CellEnvironmentState): CellState {
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

class GameOfLife(
    private val n: Int, private val m: Int,
    private val nextStep: (CellState, CellEnvironmentState) -> CellState = ::classicNextStep,
    init: (Int, Int) -> CellState = { _, _ -> CellState(false) }
) {
    private var field: List<ClassicCell>
    private val neighborsIndices = setOf(
        Pair(-1, -1), Pair(-1, 0), Pair(-1, 1),
        Pair(0, -1), Pair(0, 1),
        Pair(1, -1), Pair(1, 0), Pair(1, 1)
    )

    init {
        val tempField = Array(n) { Array<ClassicCell?>(m) { null } }

        fun getOrCreate(i: Int, j: Int): ClassicCell {
            return if (tempField[i][j] == null) {
                ClassicCell(i, j, init(i, j)).apply { tempField[i][j] = this }
            } else {
                tempField[i][j]!!
            }
        }

        for (i in 0 until n) {
            for (j in 0 until m) {
                val cell = getOrCreate(i, j)
                for ((k, l) in getNeighboursIds(i, j)) {
                    cell.addNeighbour(getOrCreate(k, l))
                }
            }
        }

        field = tempField.flatMap { it.filterNotNull() }
    }

    private fun getNeighboursIds(i: Int, j: Int): List<Pair<Int, Int>> {
        return neighborsIndices.map { cyclicMod(i - it.first, n) to cyclicMod(j - it.second, m) }
    }

    private fun cyclicMod(i: Int, n: Int): Int {
        return if (i >= 0) i % n else n + i % n
    }

    fun iterate() {
        field.forEach { it.iterate(nextStep) }
        field.forEach { it.endIteration() }
    }

    fun observe(process: (ClassicCell) -> Unit) {
        field.forEach { process(it) }
    }

    override fun toString(): String {
        val builder = StringBuilder()
        for (i in 0 until n) {
            for (j in 0 until m) {
                builder.append(if (field[i * n + j].isAlive()) "X" else "O")
            }
            builder.append("\n")
        }
        builder.append("\n")
        return builder.toString()
    }
}