package space.kscience.simba.state

import space.kscience.simba.utils.Vector

@kotlinx.serialization.Serializable
data class ActorMitosisState(val colorIntensity: Double) : ObjectState

@kotlinx.serialization.Serializable
data class ActorMitosisCell(
    override val vectorId: Vector,
    override val state: ActorMitosisState,
) : Cell<ActorMitosisCell, ActorMitosisState>() {
    override suspend fun iterate(
        convertState: suspend (ActorMitosisState, List<ActorMitosisCell>) -> ActorMitosisState
    ): ActorMitosisCell {
        return ActorMitosisCell(vectorId, convertState(state, neighbours))
    }
}
