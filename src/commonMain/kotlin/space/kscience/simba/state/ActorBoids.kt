package space.kscience.simba.state

import kotlinx.serialization.Transient
import space.kscience.simba.utils.Vector
import space.kscience.simba.utils.Vector2

@kotlinx.serialization.Serializable
data class ActorBoidsState(val position: Vector2, val direction: Vector2, val velocity: Vector2) : ObjectState

data class ActorBoidsEnvironmentState(val field: MutableList<ActorBoidsCell>) : EnvironmentState

@kotlinx.serialization.Serializable
data class ActorBoidsCell(
    val id: Int, val state: ActorBoidsState
): Cell<ActorBoidsCell, ActorBoidsState, ActorBoidsEnvironmentState>() {
    @Transient
    override val vectorId: Vector = intArrayOf(id)

    @kotlinx.serialization.Transient
    private val environmentState = ActorBoidsEnvironmentState(mutableListOf())

    override fun isReadyForIteration(expectedCount: Int): Boolean {
        return environmentState.field.size == expectedCount
    }

    override fun addNeighboursState(cell: ActorBoidsCell) {
        environmentState.field.add(cell)
    }

    override fun iterate(convert: (ActorBoidsState, ActorBoidsEnvironmentState) -> ActorBoidsState): ActorBoidsCell {
        return ActorBoidsCell(id, convert(state, environmentState))
    }
}