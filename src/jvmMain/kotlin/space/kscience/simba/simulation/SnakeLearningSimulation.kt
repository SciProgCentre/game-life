package space.kscience.simba.simulation

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import space.kscience.simba.EngineFactory
import space.kscience.simba.engine.Engine
import space.kscience.simba.machine_learning.reinforcment_learning.game.Snake
import space.kscience.simba.state.*
import space.kscience.simba.systems.PrintSystem
import space.kscience.simba.utils.Vector2
import space.kscience.simba.utils.isInsideBox
import kotlin.math.pow
import kotlin.random.Random

class SnakeLearningSimulation: Simulation<ActorSnakeCell, ActorSnakeState>("snake") {
    private val actorsCount = 100
    private val snake = Snake(gameSize.first, gameSize.second, seed)

    override val engine: Engine = createEngine()
    override val printSystem: PrintSystem<ActorSnakeState> = PrintSystem(actorsCount)

    init {
        engine.addNewSystem(printSystem)
        engine.init()
        engine.iterate()
    }

    private fun createEngine(): Engine {
        return EngineFactory.createEngine(
            intArrayOf(actorsCount),
            (1 until actorsCount).map { intArrayOf(it) }.toSet(),
            { (id) -> ActorSnakeCell(id, ActorSnakeState(id, QTable(), 0)) },
        )
    }

    override fun Routing.addAdditionalRouting() {
        get("/status/$name/play/{iteration}") {
            val iteration = call.parameters["iteration"]?.toLong() ?: error("Invalid status request")

            snake.restart()
            val history = mutableListOf<SnakeState>()
            val state = printSystem.render(iteration).first()

            fun nextDirection(currentState: SnakeState, oldDirection: Snake.Direction?): Snake.Direction {
                return state.table.getNextDirection(currentState, random.getRandomSnakeDirection(oldDirection), oldDirection, true)
            }

            var eatenBait = 0
            snake.play(100, ::nextDirection) { game, oldState, _ ->
                history += oldState
                if (game.ateBait()) eatenBait++
            }

            call.respond(eatenBait to history)
        }
    }

    companion object {
        // TODO environment
        private val gameSize = 10 to 10
        private val seed = 0
        private val random = Random(seed)
        private val maxIterations = 100
        private val trainProbability = 0.9f

        suspend fun nextState(state: ActorSnakeState, neighbours: List<ActorSnakeState>): ActorSnakeState {
            val combinedState = if (neighbours.none { it.id == 0 }) {
                val stateCopy = ActorSnakeState(state.id, state.table.deepCopy(), state.iteration)
                for (cell in neighbours) {
                    stateCopy.table.combine(cell.table)
                }
                stateCopy
            } else {
                ActorSnakeState(state.id, neighbours.first { it.id == 0 }.table.deepCopy(), state.iteration)
            }

            val snakeGame = Snake(gameSize.first, gameSize.second, random.nextInt())
            snakeGame.train(random, combinedState.table, maxIterations, trainProbability) { calculateReward(it) }

            return combinedState
        }

        private fun Snake.calculateReward(oldState: SnakeState): Double {
            val headPosition = getHeadPosition()
            val baitPosition = getBaitPosition() ?: return 1.0 // best score

            if (!headPosition.isInsideBox(width.toDouble(), height.toDouble())) return 0.0
            if (!baitPosition.isInsideBox(width.toDouble(), height.toDouble())) return 0.0

            fun getDistanceToBait(head: Vector2): Double {
                val x = (baitPosition.first - head.first)
                val y = (baitPosition.second - head.second)
                return ((x / width).pow(2) + (y / height).pow(2))
            }

            val currentDistance = getDistanceToBait(headPosition)
            val oldDistance = getDistanceToBait(oldState.body.last())

            return if (currentDistance < oldDistance) return 1.0 else -1.0
        }
    }
}
