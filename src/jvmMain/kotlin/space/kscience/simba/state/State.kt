package space.kscience.simba.state

import space.kscience.simba.utils.Vector
import space.kscience.simba.utils.compareTo
import java.io.Serializable

interface ObjectState : Serializable

interface EnvironmentState: Serializable

@kotlinx.serialization.Serializable
abstract class Cell<Self : Cell<Self, State>, State : ObjectState> : Comparable<Self>, Serializable {
    abstract val vectorId: Vector
    abstract val state: State
    private val neighbours: MutableList<State> = mutableListOf()

    open fun isReadyForIteration(expectedCount: Int): Boolean = neighbours.size == expectedCount
    open fun addNeighboursState(state: State) { neighbours += state }
    abstract suspend fun iterate(convertState: suspend (State, List<State>) -> State): Self

    protected fun getNeighboursStates() = neighbours

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
