package space.kscience.simba.simulation

import space.kscience.simba.EngineFactory
import space.kscience.simba.engine.Engine
import space.kscience.simba.state.*
import space.kscience.simba.systems.PrintSystem
import kotlin.random.Random

// original work https://github.com/MaxRobinsonTheGreat/NeuralPatterns
class MitosisSimulation: Simulation<ActorMitosisState, EnvironmentState>("mitosis") {
    private val random = Random(0)
    private val n = 100
    private val m = 100

    override val engine: Engine<EnvironmentState> = EngineFactory.createEngine(intArrayOf(n, m), gameOfLifeNeighbours) {
        ActorMitosisState(it, random.nextDouble())
    }

    override val printSystem: PrintSystem<ActorMitosisState, EnvironmentState> = PrintSystem(n * m)

    init {
        engine.addNewSystem(printSystem)
        engine.init()
        engine.iterate()
    }
}
