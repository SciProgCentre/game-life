package space.kscience.simba.state

import space.kscience.simba.utils.Vector
import space.kscience.simba.utils.compareTo

interface ObjectState

abstract class Cell<Self : Cell<Self, State>, State : ObjectState> : Comparable<Self> {
    abstract val vectorId: Vector
    abstract val state: State
    val neighbours: MutableList<Self> = mutableListOf()

    open fun isReadyForIteration(expectedCount: Int): Boolean = neighbours.size == expectedCount
    open fun addNeighboursState(cell: Self) { neighbours += cell }
    abstract fun iterate(convertState: (State, List<Self>) -> State): Self

    override fun compareTo(other: Self): Int {
        return vectorId.compareTo(other.vectorId)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Cell<*, *>

        if (!vectorId.contentEquals(other.vectorId)) return false

        return true
    }

    override fun hashCode(): Int {
        return vectorId.contentHashCode()
    }
}
