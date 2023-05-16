package space.kscience.simba.simulation

import space.kscience.simba.EngineFactory
import space.kscience.simba.engine.Engine
import space.kscience.simba.state.ActorMitosisCell
import space.kscience.simba.state.ActorMitosisState
import space.kscience.simba.state.EnvironmentState
import space.kscience.simba.state.gameOfLifeNeighbours
import space.kscience.simba.systems.PrintSystem
import space.kscience.simba.utils.Vector
import space.kscience.simba.utils.compareTo
import kotlin.math.pow
import kotlin.random.Random

// original work https://github.com/MaxRobinsonTheGreat/NeuralPatterns
class MitosisSimulation: Simulation<ActorMitosisCell, ActorMitosisState, EnvironmentState>("mitosis") {
    private val random = Random(0)
    private val n = 100
    private val m = 100

    override val engine: Engine<EnvironmentState> = EngineFactory.createEngine(
        intArrayOf(n, m), gameOfLifeNeighbours, ::nextCell
    )

    override val printSystem: PrintSystem<ActorMitosisState> = PrintSystem(n * m)

    init {
        engine.addNewSystem(printSystem)
        engine.init()
        engine.iterate()
    }

    private fun nextCell(vector: Vector): ActorMitosisCell {
        return ActorMitosisCell(vector, ActorMitosisState(vector, random.nextDouble()))
    }

    companion object {
        private val filter = doubleArrayOf(
            -0.939, 0.88, -0.939,
            0.88, 0.4, 0.88,
            -0.939, 0.88, -0.939
        )

        private fun activation(value: Double): Double {
            return -1.0 / (0.9 * value.pow(2.0) + 1.0) + 1.0
        }

        suspend fun nextStep(state: ActorMitosisState, neighbours: List<ActorMitosisState>): ActorMitosisState {
            val comparator = Comparator<ActorMitosisState> { o1, o2 -> o1.vectorId.compareTo(o2.vectorId) }
            val image = neighbours.sortedWith(comparator).map { it.colorIntensity }.toMutableList()
            image.add(filter.size / 2, state.colorIntensity)

            val convolution = filter.zip(image).sumOf { (i, j) -> i * j }
            return ActorMitosisState(state.vectorId, activation(convolution))
        }
    }
}