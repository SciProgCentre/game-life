package space.kscience.simba

class GameOfLife(private val n: Int, private val m: Int, init: (Int, Int) -> Boolean = { _, _ -> false }) {
    private var field: List<Cell>

    init {
        val tempField = Array(n) { Array<Cell?>(m) { null } }

        fun getOrCreate(i: Int, j: Int): Cell {
            return if (tempField[i][j] == null) {
                Cell(i, j, init(i, j)).apply { tempField[i][j] = this }
            } else {
                tempField[i][j]!!
            }
        }

        for (i in 0 until n) {
            for (j in 0 until n) {
                val cell = getOrCreate(i, j)
                for ((k, l) in getNeighboursIds(i, j)) {
                    cell.addNeighbour(getOrCreate(k, l))
                }
            }
        }

        field = tempField.flatMap { it.filterNotNull() }
    }

    fun iterate() {
        field.forEach { it.iterate() }
        field.forEach { it.endIteration() }
    }

    private fun getNeighboursIds(i: Int, j: Int): Set<Pair<Int, Int>> {
        return setOf(
            cyclicMod(i - 1, n) to cyclicMod(j - 1, m), cyclicMod(i - 1, n) to cyclicMod(j, m), cyclicMod(i - 1, n) to cyclicMod(j + 1, m),
            cyclicMod(i, n) to cyclicMod(j - 1, m), cyclicMod(i, n) to cyclicMod(j, m), cyclicMod(i, n) to cyclicMod(j + 1, m),
            cyclicMod(i + 1, n) to cyclicMod(j - 1, m), cyclicMod(i + 1, n) to cyclicMod(j, m), cyclicMod(i + 1, n) to cyclicMod(j + 1, m)
        )
    }

    private fun cyclicMod(i: Int, n: Int): Int {
        return if (i >= 0) i % n else n + i % n
    }

    override fun toString(): String {
        val builder = StringBuilder()
        for (i in 0 until n) {
            for (j in 0 until n) {
                builder.append(if (field[i * n + j].isAlive()) "X" else "O")
            }
            builder.append("\n")
        }
        builder.append("\n")
        return builder.toString()
    }
}