package space.kscience.simba.simulation

import io.ktor.application.*
import io.ktor.routing.*
import space.kscience.simba.EngineFactory
import space.kscience.simba.engine.Engine
import space.kscience.simba.state.ActorBoidsState
import space.kscience.simba.state.ActorBoidsState.Companion.randomBoidsState
import space.kscience.simba.state.BoidsEnvironment
import space.kscience.simba.aggregators.PrintAggregator
import kotlin.random.Random

/**
 * This simulation imitate the flocking behaviour of birds, known as boids.
 */
class BoidsSimulation : Simulation<ActorBoidsState, BoidsEnvironment>("boids") {
    private val random = Random(0)
    private val env = BoidsEnvironment()
    private val n = 100

    private val neighbours = (1 until n).map { intArrayOf(it) }.toSet()

    override val engine: Engine<BoidsEnvironment> = EngineFactory.createEngine(intArrayOf(n), neighbours) {
        random.randomBoidsState(env)
    }
    override val printAggregator: PrintAggregator<ActorBoidsState, BoidsEnvironment> = PrintAggregator(n)

    init {
        engine.addNewAggregator(printAggregator)
        engine.init()
        engine.setNewEnvironment(env)
        engine.iterate()
    }

    override fun Routing.addAdditionalRouting() {
        put("/$name") {
            val withAllRules = call.request.queryParameters["withAllRules"] == "true"
            engine.setNewEnvironment(env.copy(applyAllRules = withAllRules))
        }
    }
}