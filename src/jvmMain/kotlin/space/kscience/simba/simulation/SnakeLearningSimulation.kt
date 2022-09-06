package space.kscience.simba.simulation

import space.kscience.simba.akka.actor.AkkaActorEngine
import space.kscience.simba.engine.Engine
import space.kscience.simba.machine_learning.reinforcment_learning.game.Snake
import space.kscience.simba.state.*
import space.kscience.simba.systems.AbstractCollector
import space.kscience.simba.systems.PrintSystem
import space.kscience.simba.utils.Vector2
import space.kscience.simba.utils.isInsideBox
import kotlin.math.pow
import kotlin.random.Random

class SnakeLearningSimulation: Simulation<ActorSnakeCell, ActorSnakeState>("snake") {
    private val random = Random(0)
    private val actorsCount = 100
    private val maxIterations = 100
    private val trainProbability = 0.9f

    override val engine: Engine = createEngine()

    override val printSystem: PrintSystem<ActorSnakeCell, ActorSnakeState> = PrintSystem(actorsCount)
    private val combineSystem = CombineSystem(actorsCount)

    init {
        engine.addNewSystem(printSystem)
        engine.addNewSystem(combineSystem)
        engine.init()
        engine.iterate()
    }

    private fun createEngine(): Engine {
        return AkkaActorEngine(
            intArrayOf(actorsCount),
            (1 until actorsCount).map { intArrayOf(it) }.toSet(),
            { (id) -> ActorSnakeCell(id, ActorSnakeState(QTable(), 0)) },
            ::nextState
        )
    }

    override fun Set<Any>.transformData(): Set<Any> {
        return setOf(this.last())
    }

    // TODO maybe create new separate actor "TableManager" which is responsible to combine tables
    private suspend fun nextState(state: ActorSnakeState, neighbours: List<ActorSnakeCell>): ActorSnakeState {
        val combinedState = combineSystem.getCombinedDataFor(state.iteration + 1)

        val gameSize = 10 to 10
        val snakeGame = Snake(gameSize.first, gameSize.second, random.nextInt())
        snakeGame.train(random, combinedState, maxIterations, trainProbability) { calculateReward(it) }

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

private class CombineSystem(private val fieldSize: Int): AbstractCollector<ActorSnakeCell, ActorSnakeState>() {
    private val cache = mutableListOf<QTable<SnakeState, SnakeAction>>()
    private val lock = Object()

    override fun isCompleteFor(iteration: Long): Boolean {
        val states = tryToGetDataFor(iteration)
        return !(states == null || states.size != fieldSize)
    }

    suspend fun getCombinedDataFor(iteration: Long): ActorSnakeState {
        val allCells = getDataFor(iteration)
        synchronized(lock) {
            if (cache.size >= iteration) {
                return ActorSnakeState(cache[iteration.toInt() - 1], iteration)
            }

            val stateCopy = ActorSnakeState(allCells.first().state.table.deepCopy(), iteration)
            if (allCells.size != 1) {
                for (cell in allCells.drop(1)) {
                    stateCopy.table.combine(cell.state.table)
                }
            }
            cache.add(stateCopy.table)
        }

        return ActorSnakeState(cache[iteration.toInt() - 1], iteration)
    }
}