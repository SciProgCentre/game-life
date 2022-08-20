package space.kscience.simba.state

import space.kscience.simba.utils.Vector

@kotlinx.serialization.Serializable
data class ActorMitosisState(val colorIntensity: Double) : ObjectState

@kotlinx.serialization.Serializable
data class ActorMitosisEnv(val neighbours: MutableList<ActorMitosisCell>) : EnvironmentState {}

@kotlinx.serialization.Serializable
data class ActorMitosisCell(
    override val vectorId: Vector,
    override val state: ActorMitosisState,
    override val environmentState: ActorMitosisEnv = ActorMitosisEnv(mutableListOf())
) : Cell<ActorMitosisCell, ActorMitosisState, ActorMitosisEnv>() {
    override fun isReadyForIteration(expectedCount: Int): Boolean {
        return environmentState.neighbours.size == expectedCount
    }

    override fun addNeighboursState(cell: ActorMitosisCell) {
        environmentState.neighbours.add(cell)
    }

    override fun iterate(
        convertState: (ActorMitosisState, ActorMitosisEnv) -> ActorMitosisState,
        convertEnv: (ActorMitosisState, ActorMitosisEnv) -> ActorMitosisEnv
    ): ActorMitosisCell {
        return ActorMitosisCell(vectorId, convertState(state, environmentState), convertEnv(state, environmentState))
    }
}
