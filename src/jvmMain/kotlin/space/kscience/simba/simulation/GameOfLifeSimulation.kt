package space.kscience.simba.simulation

import space.kscience.simba.EngineFactory
import space.kscience.simba.engine.Engine
import space.kscience.simba.state.*
import space.kscience.simba.systems.PrintSystem
import kotlin.random.Random

class GameOfLifeSimulation: Simulation<ActorGameOfLifeCell, ActorGameOfLifeState>("gameOfLife") {
    private val random = Random(0)
    private val n = 100
    private val m = 100

    override val engine: Engine = EngineFactory.createEngine(
        intArrayOf(n, m), gameOfLifeNeighbours, { (i, j) -> classicCell(i, j, random.nextBoolean()) }, ::actorNextStep
    )

    override val printSystem: PrintSystem<ActorGameOfLifeCell, ActorGameOfLifeState> = PrintSystem(n * m)

    init {
        engine.addNewSystem(printSystem)
        engine.init()
        engine.iterate()
    }
}