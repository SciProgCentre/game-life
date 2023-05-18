package space.kscience.simba.simulation

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import space.kscience.simba.EngineFactory
import space.kscience.simba.engine.Engine
import space.kscience.simba.machine_learning.reinforcment_learning.game.Snake
import space.kscience.simba.state.*
import space.kscience.simba.systems.PrintSystem
import kotlin.random.Random

class SnakeLearningSimulation: Simulation<ActorSnakeState, SnakeEnvironment>("snake") {
    private val actorsCount = 30
    private val random = Random(0)
    private val initEnv = SnakeEnvironment()
    private val snake = Snake(initEnv.gameSize.first, initEnv.gameSize.second, initEnv.seed)

    override val engine: Engine<SnakeEnvironment> = createEngine()
    override val printSystem: PrintSystem<ActorSnakeState, SnakeEnvironment> = PrintSystem(actorsCount)

    init {
        engine.addNewSystem(printSystem)
        engine.init()
        engine.setNewEnvironment(initEnv)
        engine.iterate()
    }

    private fun createEngine(): Engine<SnakeEnvironment> {
        return EngineFactory.createEngine(
            intArrayOf(actorsCount), (1 until actorsCount).map { intArrayOf(it) }.toSet()
        ) { (id) -> ActorSnakeState(id, QTable(), 0) }
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

            engine.iterate()
            call.respond(eatenBait to history)
        }
    }
}
