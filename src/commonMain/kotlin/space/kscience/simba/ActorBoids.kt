package space.kscience.simba

typealias Vector2 = Pair<Double, Double>

@kotlinx.serialization.Serializable
data class ActorBoidsState(val direction: Vector2, val velocity: Vector2) : ObjectState

data class ActorBoidsEnvironmentState(val field: MutableList<ActorBoidsState>) : EnvironmentState {}

@kotlinx.serialization.Serializable
data class ActorBoidsCell(
    private val state: ActorBoidsState
): Cell<ActorBoidsCell, ActorBoidsState, ActorBoidsEnvironmentState>() {
    override fun isReadyForIteration(expectedCount: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun addNeighboursState(cell: ActorBoidsCell) {
        TODO("Not yet implemented")
    }

    override fun iterate(convert: (ActorBoidsState, ActorBoidsEnvironmentState) -> ActorBoidsState): ActorBoidsCell {
        TODO("Not yet implemented")
    }

}