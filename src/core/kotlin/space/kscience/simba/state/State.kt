package space.kscience.simba.state

import space.kscience.simba.utils.Vector
import space.kscience.simba.utils.compareTo
import java.io.Serializable

/**
 * A data container where actor can store its personal information. Data can be mutable, and it must be serializable
 * in order to pass this object to other actors.
 */
interface ObjectState<Self : ObjectState<Self, E>, E : EnvironmentState> : Serializable {
    /**
     * This method transform data and returns new state of the same type: `state[time] => state[time + 1]`
     */
    suspend fun iterate(neighbours: List<Self>, env: E?): Self

    /**
     * Check if current state has enough information to be able to iterate.
     */
    fun isReadyForIteration(neighbours: List<Self>, env: E?, expectedCount: Int): Boolean
}

/**
 * A data container that store information that is common for all agents.
 */
interface EnvironmentState : Serializable

// TODO
//  Try to isolate `Cell` from `ObjectState` and `EnvironmentState`.
//    * must decide that to do with `neighbours`
//    * must decide that to do with `isReadyForIteration`
/**
 * A data container that stores:
 *  * unique identifier of current actor
 *  * actor's state
 *  * states of actor's neighbours
 */
class Cell<S : ObjectState<S, E>, E : EnvironmentState>(val vectorId: Vector, val state: S) : Comparable<Cell<S, E>> {
    private var neighbours: MutableList<S> = mutableListOf()

    fun addNeighboursState(state: S) {
        neighbours += state
    }

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
