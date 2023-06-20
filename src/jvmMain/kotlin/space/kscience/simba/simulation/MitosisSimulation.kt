package space.kscience.simba.simulation

import space.kscience.simba.EngineFactory
import space.kscience.simba.engine.Engine
import space.kscience.simba.state.*
import space.kscience.simba.aggregators.PrintAggregator
import kotlin.random.Random

// original work https://github.com/MaxRobinsonTheGreat/NeuralPatterns
/**
 * This simulation is basically a convolution with different parameters. Changing these parameters we can get something
 * that is pretty much similar to vital activity of living cells, for example mitosis process.
 */
class MitosisSimulation : Simulation<ActorMitosisState, EnvironmentState>("mitosis") {
    private val random = Random(0)
    private val n = 100
    private val m = 100

    override val engine: Engine<EnvironmentState> = EngineFactory.createEngine(intArrayOf(n, m), gameOfLifeNeighbours) {
        ActorMitosisState(it, random.nextDouble())
    }

    override val printAggregator: PrintAggregator<ActorMitosisState, EnvironmentState> = PrintAggregator(n * m)

    init {
        engine.addNewAggregator(printAggregator)
        engine.init()
        engine.iterate()
    }
}
