package space.kscience.simba

@kotlinx.serialization.Serializable
data class ActorBoidsState(val position: Vector2, val direction: Vector2, val velocity: Vector2) : ObjectState

data class ActorBoidsEnvironmentState(val field: MutableList<ActorBoidsCell>) : EnvironmentState

@kotlinx.serialization.Serializable
data class ActorBoidsCell(
    val id: Int, val state: ActorBoidsState
): Cell<ActorBoidsCell, ActorBoidsState, ActorBoidsEnvironmentState>() {
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

    override fun compareTo(other: ActorBoidsCell): Int {
        return id.compareTo(other.id)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ActorBoidsCell

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id
    }
}