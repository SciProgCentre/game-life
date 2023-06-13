package space.kscience.simba.state

import space.kscience.simba.utils.Vector
import space.kscience.simba.utils.compareTo
import kotlin.math.pow

@kotlinx.serialization.Serializable
data class ActorMitosisState(val vectorId: Vector, val colorIntensity: Double) : ObjectState<ActorMitosisState, EnvironmentState> {
    override suspend fun iterate(neighbours: List<ActorMitosisState>, env: EnvironmentState?): ActorMitosisState {
        val comparator = Comparator<ActorMitosisState> { o1, o2 -> o1.vectorId.compareTo(o2.vectorId) }
        val image = neighbours.sortedWith(comparator).map { it.colorIntensity }.toMutableList()
        image.add(filter.size / 2, this.colorIntensity)

        val convolution = filter.zip(image).sumOf { (i, j) -> i * j }
        return ActorMitosisState(this.vectorId, activation(convolution))
    }

    override fun isReadyForIteration(neighbours: List<ActorMitosisState>, env: EnvironmentState?, expectedCount: Int): Boolean {
        return neighbours.size == expectedCount
    }

    @kotlinx.serialization.Transient
    private val filter = doubleArrayOf(
        -0.939, 0.88, -0.939,
        0.88, 0.4, 0.88,
        -0.939, 0.88, -0.939
    )

    private fun activation(value: Double): Double {
        return -1.0 / (0.9 * value.pow(2.0) + 1.0) + 1.0
    }
}
