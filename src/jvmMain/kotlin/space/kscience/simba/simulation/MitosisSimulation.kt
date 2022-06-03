package space.kscience.simba.simulation

import space.kscience.simba.akka.actor.AkkaActorEngine
import space.kscience.simba.engine.Engine
import space.kscience.simba.state.*
import space.kscience.simba.systems.PrintSystem
import space.kscience.simba.utils.Vector
import kotlin.math.pow
import kotlin.random.Random

// original work https://github.com/MaxRobinsonTheGreat/NeuralPatterns
class MitosisSimulation: Simulation<ActorMitosisCell, ActorMitosisState, ActorMitosisEnv>("mitosis") {
    private val random = Random(0)
    private val n = 100
    private val m = 100

    private val filter = doubleArrayOf(
        -0.939, 0.88, -0.939,
        0.88, 0.4, 0.88,
        -0.939, 0.88, -0.939
    )

    override val engine: Engine = AkkaActorEngine(intArrayOf(n, m), gameOfLifeNeighbours, ::nextCell, ::nextStep)

    override val printSystem: PrintSystem<ActorMitosisCell, ActorMitosisState, ActorMitosisEnv> = PrintSystem(n * m)

    init {
        engine.addNewSystem(printSystem)
        engine.init()
        engine.iterate()
    }

    private fun activation(value: Double): Double {
        return -1.0 / (0.9 * value.pow(2.0) + 1.0) + 1.0
    }

    private fun nextCell(vector: Vector): ActorMitosisCell {
        return ActorMitosisCell(vector, ActorMitosisState(random.nextDouble()))
    }

    private fun nextStep(state: ActorMitosisState, environmentState: ActorMitosisEnv): ActorMitosisState {
        val image = environmentState.neighbours.sorted().map { it.state.colorIntensity }.toMutableList()
        image.add(filter.size / 2, state.colorIntensity)

        val convolution = filter.zip(image).sumOf { (i, j) -> i * j }
        return ActorMitosisState(activation(convolution))
    }
}