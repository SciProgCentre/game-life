package space.kscience.simba.state

import space.kscience.simba.utils.Vector

@kotlinx.serialization.Serializable
data class ActorMitosisState(val vectorId: Vector, val colorIntensity: Double) : ObjectState

data class ActorMitosisCell(
    override val vectorId: Vector,
    override val state: ActorMitosisState,
) : Cell<ActorMitosisCell, ActorMitosisState>() {
    override suspend fun iterate(
        convertState: suspend (ActorMitosisState, List<ActorMitosisState>) -> ActorMitosisState
    ): ActorMitosisCell {
        return ActorMitosisCell(vectorId, convertState(state, getNeighboursStates()))
    }
}
