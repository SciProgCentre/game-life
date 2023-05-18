package space.kscience.simba.simulation

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import space.kscience.simba.engine.Engine
import space.kscience.simba.state.*
import space.kscience.simba.systems.PrintSystem

abstract class Simulation<State: ObjectState<State, Env>, Env: EnvironmentState>(protected val name: String) {
    protected abstract val engine: Engine<Env>
    protected abstract val printSystem: PrintSystem<State, Env>

    protected open fun Routing.addAdditionalRouting() {}
    protected open fun Set<Any>.transformData(): Set<Any> = this

    fun Routing.setUp() {
        get("/status/$name/{iteration}") {
            val iteration = call.parameters["iteration"]?.toLong() ?: error("Invalid status request")
            if (!printSystem.isCompleteFor(iteration)) engine.iterate()
            call.respond((printSystem.render(iteration) as Set<State>).transformData())
        }

        addAdditionalRouting()
    }
}