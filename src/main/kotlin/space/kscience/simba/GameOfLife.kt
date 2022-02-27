package space.kscience.simba

class GameOfLife(private val n: Int, private val m: Int, init: (Int, Int) -> Boolean = { _, _ -> false }) {
    var field = Array(n) { i -> BooleanArray(m) { j -> init(i, j) } }

    fun iterate() {
        for (i in 0 until n) {
            for (j in 0 until n) {
                val aliveNeighbours = getAliveNeighboursCount(i, j)
                if (isAlive(i, j)) {
                    if (aliveNeighbours != 2 && aliveNeighbours != 3) {
                        field[i][j] = false
                    }
                } else if (aliveNeighbours == 3) {
                    field[i][j] = true
                }
            }
        }
    }

    private fun getAliveNeighboursCount(i: Int, j: Int): Int {
        return listOf(
            getValueAt(i - 1, j - 1), getValueAt(i - 1, j), getValueAt(i - 1, j + 1),
            getValueAt(i, j - 1), getValueAt(i, j), getValueAt(i, j + 1),
            getValueAt(i + 1, j - 1), getValueAt(i + 1, j), getValueAt(i + 1, j + 1)
        ).count { it }
    }

    private fun cyclicMod(i: Int, n: Int): Int {
        return if (i >= 0) i % n else n + i % n
    }

    private fun getValueAt(i: Int, j: Int): Boolean {
        return field[cyclicMod(i, n)][cyclicMod(j, m)]
    }

    private fun isAlive(i: Int, j: Int) = field[i][j]

    override fun toString(): String {
        return field.joinToString(separator = "\n", postfix = "\n") { array ->
            array.joinToString(separator = "") { if (it) "X" else "O" }
        }
    }
}