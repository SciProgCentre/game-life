package space.kscience.simba.simulation

import space.kscience.simba.EngineFactory
import space.kscience.simba.engine.Engine
import space.kscience.simba.state.*
import space.kscience.simba.systems.PrintSystem
import kotlin.random.Random

class GameOfLifeSimulation: Simulation<ActorGameOfLifeCell, ActorGameOfLifeState, EnvironmentState>("gameOfLife") {
    private val random = Random(0)
    private val n = 10
    private val m = 10

    override val engine: Engine<EnvironmentState> = EngineFactory.createEngine(
        intArrayOf(n, m), gameOfLifeNeighbours, { (i, j) -> classicCell(i, j, random.nextBoolean()) }, //::actorNextStep
    )

    override val printSystem: PrintSystem<ActorGameOfLifeState> = PrintSystem(n * m)

    init {
        engine.addNewSystem(printSystem)
        engine.init()
        engine.iterate()
    }
}