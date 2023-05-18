package space.kscience.simba.state

import space.kscience.simba.utils.Vector
import space.kscience.simba.utils.compareTo

interface ObjectState<Self: ObjectState<Self, E>, E: EnvironmentState> {
    suspend fun iterate(neighbours: List<Self>, env: E?): Self
    fun isReadyForIteration(neighbours: List<Self>, env: E?, expectedCount: Int): Boolean
}

interface EnvironmentState

// TODO
//  1. try to isolate Cell from State and Env.
//      1.1 must decide that to do with `neighbours`
//      1.2 must decide that to do with `isReadyForIteration`
class Cell<S: ObjectState<S, E>, E: EnvironmentState>(val vectorId: Vector, val state: S) : Comparable<Cell<S, E>> {
    private var neighbours: MutableList<S> = mutableListOf()

    fun addNeighboursState(state: S) { neighbours += state }

    fun isReadyForIteration(env: E?, expectedCount: Int): Boolean {
        return state.isReadyForIteration(neighbours, env, expectedCount)
    }

    // TODO reconsider
    suspend fun iterate(env: E?): Cell<S, E> {
        return Cell(vectorId, state.iterate(neighbours, env))
    }

    override fun compareTo(other: Cell<S, E>): Int {
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
