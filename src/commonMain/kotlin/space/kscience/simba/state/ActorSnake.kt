package space.kscience.simba.state

import space.kscience.simba.machine_learning.reinforcment_learning.game.Snake
import space.kscience.simba.utils.Vector
import space.kscience.simba.utils.Vector2
import kotlin.random.Random

@kotlinx.serialization.Serializable
data class ActorSnakeState(val table: QTable<SnakeState, SnakeAction>) : ObjectState

data class ActorSnakeEnv(
    val neighbours: MutableList<ActorSnakeCell>
) : EnvironmentState {}

@kotlinx.serialization.Serializable
data class ActorSnakeCell(
    val id: Int, val state: ActorSnakeState
) : Cell<ActorSnakeCell, ActorSnakeState, ActorSnakeEnv>() {
    @kotlinx.serialization.Transient
    override val vectorId: Vector = intArrayOf(id)

    @kotlinx.serialization.Transient
    private val environmentState = ActorSnakeEnv(mutableListOf())

    override fun isReadyForIteration(expectedCount: Int): Boolean {
        return environmentState.neighbours.size == expectedCount
    }

    override fun addNeighboursState(cell: ActorSnakeCell) {
        environmentState.neighbours.add(cell)
    }

    override fun iterate(convert: (ActorSnakeState, ActorSnakeEnv) -> ActorSnakeState): ActorSnakeCell {
        return ActorSnakeCell(id, convert(state, environmentState))
    }
}

fun Snake.play(
    state: ActorSnakeState,
    maxIterations: Int,
    nextDirection: (QTable<SnakeState, SnakeAction>, SnakeState, Snake.Direction?) -> Snake.Direction,
    afterEachIteration : ((Snake, SnakeState, SnakeAction) -> Unit)? = null,
) {
    var iteration = 0
    var direction: Snake.Direction? = null
    while (!isGameOver() && iteration < maxIterations) {
        iteration++
        val currentState = SnakeState(getBodyWithHead(), getBaitPosition())
        direction = nextDirection(state.table, currentState, direction)
        move(direction)

        afterEachIteration?.let { it(this, currentState, SnakeAction(direction)) }
    }
}

fun Snake.train(
    random: Random,
    state: ActorSnakeState,
    maxIterations: Int,
    trainProbability: Float,
    rewardFunction : (Snake.() -> Double)
) {
    fun nextDirection(qTable: QTable<SnakeState, SnakeAction>, currentState: SnakeState, oldDirection: Snake.Direction?): Snake.Direction {
        return qTable.getNextDirection(
            currentState, random.getRandomSnakeDirection(oldDirection), random.nextDouble() >= trainProbability
        )
    }

    // play and train
    play(state, maxIterations, ::nextDirection) { _, currentState, action ->
        val reward = this.rewardFunction()
        val nextState = SnakeState(getBodyWithHead(), getBaitPosition())
        state.table.update(currentState, nextState, action, reward)
    }
}

fun Random.getRandomSnakeDirection(lastDirection: Snake.Direction?): Snake.Direction {
    var randomDirection: Snake.Direction
    do {
        randomDirection = Snake.Direction.values()[this.nextInt(0, 3)]
    } while (randomDirection == lastDirection?.getOpposite())
    return randomDirection
}

private fun QTable<SnakeState, SnakeAction>.getNextDirection(
    currentState: SnakeState, randomDirection: Snake.Direction, useBestKnownAction: Boolean
): Snake.Direction {
    return if (useBestKnownAction) {
        getBestActionForGivenState(currentState)?.direction ?: randomDirection
    } else {
        randomDirection
    }
}

interface State
interface Action

@kotlinx.serialization.Serializable
class QTable<S: State, A: Action>(private val learningRate: Double = 0.1, private val discountFactor: Double = 0.5) {
    private val qTable = mutableMapOf<S, MutableMap<A, Double>>()
    private val dataInOrder = mutableListOf<Triple<S, A, Double>>()

    fun getBestActionForGivenState(state: S): A? {
        return qTable[state]?.maxByOrNull { it.value }?.key
    }

    fun update(currentState: S, nextState: S, action: A, reward: Double) {
        val oldValue = qTable[currentState]?.get(action) ?: 0.0
        val bestValueForNextState = qTable[nextState]?.maxByOrNull { it.value }?.value ?: 0.0
        qTable.getOrPut(currentState) { mutableMapOf() }.getOrPut(action) { 0.0 }

        val newValue = (1 - learningRate) * oldValue + learningRate * (reward + discountFactor * bestValueForNextState)
        qTable[currentState]?.set(action, newValue)

        dataInOrder += Triple(currentState, action, reward)
    }

    fun combine(other: QTable<S, A>) {
        for (i in 0 until other.dataInOrder.size - 1) {
            val (currentState, action, reward) = other.dataInOrder[i]
            update(currentState, other.dataInOrder[i + 1].first, action, reward)
        }
    }
}

@kotlinx.serialization.Serializable
data class SnakeState(val body: List<Vector2>, val bait: Vector2?): State
@kotlinx.serialization.Serializable
data class SnakeAction(val direction: Snake.Direction): Action
