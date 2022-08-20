package space.kscience.simba.state

import space.kscience.simba.utils.Vector
import space.kscience.simba.utils.compareTo

interface EnvironmentState
interface ObjectState

abstract class Cell<Self : Cell<Self, State, Env>, State : ObjectState, Env : EnvironmentState> : Comparable<Self> {
    abstract val vectorId: Vector
    abstract val state: State
    abstract val environmentState: Env

    abstract fun isReadyForIteration(expectedCount: Int): Boolean
    abstract fun iterate(convertState: (State, Env) -> State, convertEnv: (State, Env) -> Env): Self
    abstract fun addNeighboursState(cell: Self)

    override fun compareTo(other: Self): Int {
        return vectorId.compareTo(other.vectorId)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Cell<*, *, *>

        if (!vectorId.contentEquals(other.vectorId)) return false

        return true
    }

    override fun hashCode(): Int {
        return vectorId.contentHashCode()
    }
}
