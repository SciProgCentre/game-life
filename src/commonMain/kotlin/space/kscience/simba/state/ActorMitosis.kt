package space.kscience.simba.state

import space.kscience.simba.utils.Vector

@kotlinx.serialization.Serializable
data class ActorMitosisState(val colorIntensity: Double) : ObjectState

data class ActorMitosisEnv(val neighbours: MutableList<ActorMitosisCell>) : EnvironmentState {}

@kotlinx.serialization.Serializable
data class ActorMitosisCell(
    override val vectorId: Vector, val state: ActorMitosisState
) : Cell<ActorMitosisCell, ActorMitosisState, ActorMitosisEnv>() {
    @kotlinx.serialization.Transient
    private val environmentState = ActorMitosisEnv(mutableListOf())

    override fun isReadyForIteration(expectedCount: Int): Boolean {
        return environmentState.neighbours.size == expectedCount
    }

    override fun addNeighboursState(cell: ActorMitosisCell) {
        environmentState.neighbours.add(cell)
    }

    override fun iterate(convert: (ActorMitosisState, ActorMitosisEnv) -> ActorMitosisState): ActorMitosisCell {
        return ActorMitosisCell(vectorId, convert(state, environmentState))
    }
}
