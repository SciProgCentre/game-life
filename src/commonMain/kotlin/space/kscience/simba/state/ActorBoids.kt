package space.kscience.simba.state

import space.kscience.simba.utils.Vector
import space.kscience.simba.utils.Vector2

@kotlinx.serialization.Serializable
data class ActorBoidsState(val position: Vector2, val direction: Vector2, val velocity: Vector2) : ObjectState

@kotlinx.serialization.Serializable
data class ActorBoidsCell(
    val id: Int,
    override val state: ActorBoidsState,
): Cell<ActorBoidsCell, ActorBoidsState>() {
    override val vectorId: Vector = intArrayOf(id)

    override suspend fun iterate(
        convertState: suspend (ActorBoidsState, List<ActorBoidsCell>) -> ActorBoidsState,
    ): ActorBoidsCell {
        return ActorBoidsCell(id, convertState(state, neighbours))
    }
}