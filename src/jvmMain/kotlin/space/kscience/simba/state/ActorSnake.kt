package space.kscience.simba.state

import space.kscience.simba.machine_learning.reinforcment_learning.game.Snake
import space.kscience.simba.simulation.SnakeLearningSimulation
import space.kscience.simba.utils.Vector2
import space.kscience.simba.utils.isInsideBox
import java.io.Serializable
import kotlin.math.pow
import kotlin.random.Random

@kotlinx.serialization.Serializable
data class ActorSnakeState(val id: Int, val table: QTable<SnakeState, SnakeAction>, val iteration: Long) : ObjectState<ActorSnakeState, EnvironmentState> {
    override suspend fun iterate(neighbours: List<ActorSnakeState>, env: EnvironmentState?): ActorSnakeState {
        val combinedState = if (neighbours.none { it.id == 0 }) {
            val stateCopy = ActorSnakeState(this.id, this.table.deepCopy(), this.iteration)
            for (cell in neighbours) {
                stateCopy.table.combine(cell.table)
            }
            stateCopy
        } else {
            ActorSnakeState(this.id, neighbours.first { it.id == 0 }.table.deepCopy(), this.iteration)
        }

        val snakeGame = Snake(SnakeLearningSimulation.gameSize.first, SnakeLearningSimulation.gameSize.second, SnakeLearningSimulation.random.nextInt())
        snakeGame.train(
            SnakeLearningSimulation.random, combinedState.table,
            SnakeLearningSimulation.maxIterations,
            SnakeLearningSimulation.trainProbability
        ) { calculateReward(it) }

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

    override fun isReadyForIteration(neighbours: List<ActorSnakeState>, env: EnvironmentState?, expectedCount: Int): Boolean {
        return if (id == 0) {
            neighbours.size == expectedCount
        } else {
            neighbours.any { it.id == 0 }
        }
    }
}

fun Snake.play(
    maxIterations: Int,
    nextDirection: (SnakeState, oldDirection: Snake.Direction?) -> Snake.Direction,
    afterEachIteration : ((Snake, oldState: SnakeState, SnakeAction) -> Unit)? = null,
) {
    var iteration = 0
    var direction: Snake.Direction? = null
    while (!isGameOver() && iteration < maxIterations) {
        iteration++
        val currentState = SnakeState(getBodyWithHead(), getBaitPosition())
        direction = nextDirection(currentState, direction)
        move(direction)

        afterEachIteration?.let { it(this, currentState, SnakeAction(direction)) }
    }
}

fun Snake.train(
    random: Random,
    qTable: QTable<SnakeState, SnakeAction>,
    maxIterations: Int,
    trainProbability: Float,
    rewardFunction : (Snake.(oldState: SnakeState) -> Double)
) {
    fun nextDirection(currentState: SnakeState, oldDirection: Snake.Direction?): Snake.Direction {
        return qTable.getNextDirection(
            currentState, random.getRandomSnakeDirection(oldDirection), oldDirection, random.nextDouble() >= trainProbability
        )
    }

    // play and train
    play(maxIterations, ::nextDirection) { game, oldState, action ->
        val reward = game.rewardFunction(oldState)
        val nextState = SnakeState(getBodyWithHead(), getBaitPosition())
        qTable.update(oldState, nextState, action, reward)
    }
}

fun Random.getRandomSnakeDirection(lastDirection: Snake.Direction?): Snake.Direction {
    var randomDirection: Snake.Direction
    do {
        randomDirection = Snake.Direction.values()[this.nextInt(0, 4)]
    } while (randomDirection == lastDirection?.getOpposite())
    return randomDirection
}

fun QTable<SnakeState, SnakeAction>.getNextDirection(
    currentState: SnakeState, nextDirection: Snake.Direction, oldDirection: Snake.Direction?, useBestKnownAction: Boolean
): Snake.Direction {
    if (!useBestKnownAction) return nextDirection
    val bestAction = getBestActionForGivenState(currentState)?.direction
    return if (bestAction == null || bestAction == oldDirection?.getOpposite()) nextDirection else bestAction
}

interface State: Serializable
interface Action: Serializable

@kotlinx.serialization.Serializable
class QTable<S: State, A: Action>(private val learningRate: Double = 0.1, private val discountFactor: Double = 0.5): Serializable {
    private val qTable = mutableMapOf<S, MutableMap<A, Double>>()

    fun getBestActionForGivenState(state: S): A? {
        return qTable[state]?.maxByOrNull { it.value }?.key
    }

    fun update(currentState: S, nextState: S, action: A, reward: Double) {
        val oldValue = qTable[currentState]?.get(action) ?: 0.0
        val bestValueForNextState = qTable[nextState]?.maxByOrNull { it.value }?.value ?: 0.0
        qTable.getOrPut(currentState) { mutableMapOf() }.getOrPut(action) { 0.0 }

        val newValue = (1 - learningRate) * oldValue + learningRate * (reward + discountFactor * bestValueForNextState)
        qTable[currentState]?.set(action, newValue)
    }

    fun combine(other: QTable<S, A>) {
        other.qTable.forEach { (key, value) ->
            if (!qTable.containsKey(key)) {
                qTable[key] = value
            } else {
                value.forEach { (action, reward) ->
                    if (qTable[key]?.containsKey(action) != true) {
                        qTable[key]?.set(action, reward)
                    } else {
                        qTable[key]?.set(action, (qTable[key]!![action]!! + reward) / 2.0)
                    }
                }
            }
        }
    }

    fun deepCopy(): QTable<S, A> {
        return QTable<S, A>(learningRate, discountFactor).apply {  ->
            this@QTable.qTable.forEach { (key, value) -> this.qTable[key] = value.toMutableMap() }
        }
    }
}

@kotlinx.serialization.Serializable
data class SnakeState(val body: List<Vector2>, val bait: Vector2?): State
@kotlinx.serialization.Serializable
data class SnakeAction(val direction: Snake.Direction): Action
