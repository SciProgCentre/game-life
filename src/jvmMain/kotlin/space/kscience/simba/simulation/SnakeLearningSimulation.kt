package space.kscience.simba.simulation

import space.kscience.simba.akka.actor.AkkaActorEngine
import space.kscience.simba.engine.Engine
import space.kscience.simba.machine_learning.reinforcment_learning.game.Snake
import space.kscience.simba.state.*
import space.kscience.simba.systems.PrintSystem
import space.kscience.simba.utils.isInsideBox
import kotlin.math.pow
import kotlin.random.Random

class SnakeLearningSimulation: Simulation<ActorSnakeCell, ActorSnakeState, ActorSnakeEnv>("snake") {
    private val random = Random(0)
    private val actorsCount = 2
    private val maxIterations = 10_000
    private val trainProbability = 0.9f

    override val engine: Engine = createEngine()

    override val printSystem: PrintSystem<ActorSnakeCell, ActorSnakeState, ActorSnakeEnv> = PrintSystem(actorsCount)

    init {
        engine.addNewSystem(printSystem)
        engine.init()
        engine.iterate()
    }

    private fun createEngine(): Engine {
        return AkkaActorEngine(
            intArrayOf(actorsCount),
            (1 until actorsCount).map { intArrayOf(it) }.toSet(),
            { (id) -> ActorSnakeCell(id, ActorSnakeState(QTable())) },
            ::nextState
        )
    }

    private fun nextState(state: ActorSnakeState, env: ActorSnakeEnv): ActorSnakeState {
        env.neighbours.forEach { state.table.combine(it.state.table) }

        val gameSize = 10 to 10
        val snakeGame = Snake(gameSize.first, gameSize.second, random.nextInt())
        snakeGame.train(random, state, maxIterations, trainProbability) { calculateReward() }

        return state
    }

    private fun Snake.calculateReward(): Double {
        val headPosition = getHeadPosition()
        val baitPosition = getBaitPosition() ?: return 1.0 // best score

        if (!headPosition.isInsideBox(width.toDouble(), height.toDouble())) return 0.0
        if (!baitPosition.isInsideBox(width.toDouble(), height.toDouble())) return 0.0

        val x = (baitPosition.first - headPosition.first)
        val y = (baitPosition.second - headPosition.second)
        return (x / width).pow(2) + (y / height).pow(2)
    }
}