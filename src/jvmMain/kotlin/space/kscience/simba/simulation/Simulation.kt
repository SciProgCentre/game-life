package space.kscience.simba.simulation

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import space.kscience.simba.engine.Engine
import space.kscience.simba.state.*
import space.kscience.simba.aggregators.PrintAggregator
import space.kscience.simba.utils.SimulationSettings

abstract class Simulation<State: ObjectState<State, Env>, Env: EnvironmentState>(protected val name: String) {
    protected abstract val engine: Engine<Env>
    protected abstract val printAggregator: PrintAggregator<State, Env>

    protected open fun Routing.addAdditionalRouting() {}
    protected open suspend fun PipelineContext<Unit, ApplicationCall>.configureSettingRouting() {
        call.respond(SimulationSettings(name, engine.dimensions))
    }

    protected open fun Set<Any>.transformData(): Set<Any> = this

    fun Routing.setUp() {
        get("/status/$name/{iteration}") {
            val iteration = call.parameters["iteration"]?.toLong() ?: error("Invalid status request")
            if (!printAggregator.isCompleteFor(iteration)) engine.iterate()
            call.respond((printAggregator.render(iteration) as Set<State>).transformData())
        }

        get("/settings") {
            configureSettingRouting()
        }

        addAdditionalRouting()
    }
}