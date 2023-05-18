package space.kscience.simba.simulation

import io.ktor.application.*
import io.ktor.routing.*
import space.kscience.simba.EngineFactory
import space.kscience.simba.engine.Engine
import space.kscience.simba.state.ActorBoidsState
import space.kscience.simba.state.ActorBoidsState.Companion.randomBoidsState
import space.kscience.simba.state.EnvironmentState
import space.kscience.simba.systems.PrintSystem
import kotlin.random.Random

class BoidsSimulation: Simulation<ActorBoidsState, EnvironmentState>("boids") {
    private val random = Random(0)
    private val n = 100

    private val neighbours = (1 until n).map { intArrayOf(it) }.toSet()
    private var withAllRules = false

    override val engine: Engine<EnvironmentState> = EngineFactory.createEngine(intArrayOf(n), neighbours) {
        random.randomBoidsState()
    }
    override val printSystem: PrintSystem<ActorBoidsState, EnvironmentState> = PrintSystem(n)

    init {
        engine.addNewSystem(printSystem)
        engine.init()
        engine.iterate()
    }

    override fun Routing.addAdditionalRouting() {
        put("/$name") {
            // TODO apply to BoidsSettings
            withAllRules = call.request.queryParameters["withAllRules"] == "true"
        }
    }
}