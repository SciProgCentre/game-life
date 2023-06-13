package space.kscience.simba.simulation

import space.kscience.simba.EngineFactory
import space.kscience.simba.engine.Engine
import space.kscience.simba.state.*
import space.kscience.simba.aggregators.PrintAggregator
import kotlin.random.Random

class GameOfLifeSimulation: Simulation<ActorGameOfLifeState, EnvironmentState>("gameOfLife") {
    private val random = Random(0)
    private val n = 10
    private val m = 10

    override val engine: Engine<EnvironmentState> = EngineFactory
        .createEngine(intArrayOf(n, m), gameOfLifeNeighbours) { (i, j) ->
            classicState(i, j, random.nextBoolean())
        }

    override val printAggregator: PrintAggregator<ActorGameOfLifeState, EnvironmentState> = PrintAggregator(n * m)

    init {
        engine.addNewAggregator(printAggregator)
        engine.init()
        engine.iterate()
    }
}