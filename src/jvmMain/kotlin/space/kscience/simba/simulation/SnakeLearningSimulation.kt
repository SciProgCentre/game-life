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

class SnakeLearningSimulation: Simulation<ActorSnakeState, EnvironmentState>("snake") {
    private val actorsCount = 30
    private val snake = Snake(gameSize.first, gameSize.second, seed)

    override val engine: Engine<EnvironmentState> = createEngine()
    override val printSystem: PrintSystem<ActorSnakeState, EnvironmentState> = PrintSystem(actorsCount)

    init {
        engine.addNewSystem(printSystem)
        engine.init()
        engine.iterate()
    }

    private fun createEngine(): Engine<EnvironmentState> {
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

    companion object {
        // TODO environment
        public val gameSize = 10 to 10
        public val seed = 0
        public val random = Random(seed)
        public val maxIterations = 100
        public val trainProbability = 0.9f
    }
}
