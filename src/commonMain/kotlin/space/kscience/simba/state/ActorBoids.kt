package space.kscience.simba.state

import kotlinx.serialization.Transient
import space.kscience.simba.utils.Vector
import space.kscience.simba.utils.Vector2

@kotlinx.serialization.Serializable
data class ActorBoidsState(val position: Vector2, val direction: Vector2, val velocity: Vector2) : ObjectState

@kotlinx.serialization.Serializable
data class ActorBoidsEnvironmentState(val field: MutableList<ActorBoidsCell>) : EnvironmentState

@kotlinx.serialization.Serializable
data class ActorBoidsCell(
    val id: Int,
    override val state: ActorBoidsState,
    override val environmentState: ActorBoidsEnvironmentState = ActorBoidsEnvironmentState(mutableListOf())
): Cell<ActorBoidsCell, ActorBoidsState, ActorBoidsEnvironmentState>() {
    override val vectorId: Vector = intArrayOf(id)

    override fun isReadyForIteration(expectedCount: Int): Boolean {
        return environmentState.field.size == expectedCount
    }

    override fun addNeighboursState(cell: ActorBoidsCell) {
        environmentState.field.add(cell)
    }

    override fun iterate(
        convertState: (ActorBoidsState, ActorBoidsEnvironmentState) -> ActorBoidsState,
        convertEnv: (ActorBoidsState, ActorBoidsEnvironmentState) -> ActorBoidsEnvironmentState
    ): ActorBoidsCell {
        return ActorBoidsCell(id, convertState(state, environmentState), convertEnv(state, environmentState))
    }
}