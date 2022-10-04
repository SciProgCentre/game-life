package space.kscience.simba.simulation

import space.kscience.simba.EngineFactory
import space.kscience.simba.engine.Engine
import space.kscience.simba.machine_learning.reinforcment_learning.game.Snake
import space.kscience.simba.state.ActorSnakeCellWithSingleManager
import space.kscience.simba.state.ActorSnakeState
import space.kscience.simba.state.QTable
import space.kscience.simba.state.train
import space.kscience.simba.systems.PrintSystem
import kotlin.random.Random

class SnakeLearningWithManagerSimulation: Simulation<ActorSnakeCellWithSingleManager, ActorSnakeState>("snakeWithManager") {
    private val random = Random(0)
    private val actorsCount = 100
    private val maxIterations = 100
    private val trainProbability = 0.9f

    override val engine: Engine = createEngine()

    override val printSystem: PrintSystem<ActorSnakeCellWithSingleManager, ActorSnakeState> = PrintSystem(actorsCount)

    init {
        engine.addNewSystem(printSystem)
        engine.init()
        engine.iterate()
    }

    private fun createEngine(): Engine {
        return EngineFactory.createEngine(
            intArrayOf(actorsCount),
            (1 until actorsCount).map { intArrayOf(it) }.toSet(),
            { (id) -> ActorSnakeCellWithSingleManager(id, ActorSnakeState(QTable(), 0)) },
            ::nextState
        )
    }

    override fun Set<Any>.transformData(): Set<Any> {
        return setOf(this.last())
    }

    private fun nextState(state: ActorSnakeState, neighbours: List<ActorSnakeCellWithSingleManager>): ActorSnakeState {
        val combinedState = if (neighbours.none { it.id == 0 }) {
            val stateCopy = ActorSnakeState(state.table.deepCopy(), state.iteration)
            for (cell in neighbours) {
                stateCopy.table.combine(cell.state.table)
            }
            stateCopy
        } else {
            ActorSnakeState(neighbours.first { it.id == 0 }.state.table.deepCopy(), state.iteration)
        }

        val gameSize = 10 to 10
        val snakeGame = Snake(gameSize.first, gameSize.second, random.nextInt())
        snakeGame.train(random, combinedState, maxIterations, trainProbability) { calculateReward(it) }

        return combinedState
    }
}
