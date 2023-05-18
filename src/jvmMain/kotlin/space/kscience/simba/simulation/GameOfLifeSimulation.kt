package space.kscience.simba.simulation

import space.kscience.simba.EngineFactory
import space.kscience.simba.engine.Engine
import space.kscience.simba.state.*
import space.kscience.simba.systems.PrintSystem
import kotlin.random.Random

class GameOfLifeSimulation: Simulation<ActorGameOfLifeState, EnvironmentState>("gameOfLife") {
    private val random = Random(0)
    private val n = 10
    private val m = 10

    override val engine: Engine<EnvironmentState> = EngineFactory.createEngine(
        intArrayOf(n, m), gameOfLifeNeighbours, { (i, j) -> classicState(i, j, random.nextBoolean()) }
    )

    override val printSystem: PrintSystem<ActorGameOfLifeState, EnvironmentState> = PrintSystem(n * m)

    init {
        engine.addNewSystem(printSystem)
        engine.init()
        engine.iterate()
    }
}