package space.kscience.simba.utils

import kotlinx.serialization.Serializable

@Serializable
data class SimulationSettings(val name: String, val dimensions: Vector) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SimulationSettings

        if (name != other.name) return false
        return dimensions.contentEquals(other.dimensions)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + dimensions.contentHashCode()
        return result
    }
}
