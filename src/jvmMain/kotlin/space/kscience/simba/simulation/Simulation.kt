package space.kscience.simba.simulation

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import space.kscience.simba.engine.Engine
import space.kscience.simba.state.Cell
import space.kscience.simba.state.ObjectState
import space.kscience.simba.systems.PrintSystem

abstract class Simulation<C: Cell<C, State>, State: ObjectState>(private val name: String) {
    protected abstract val engine: Engine
    protected abstract val printSystem: PrintSystem<C, State>

    protected open fun Routing.addAdditionalRouting() {}
    protected open fun Set<Any>.transformData(): Set<Any> = this

    fun Routing.setUp() {
        get("/status/$name/{iteration}") {
            val iteration = call.parameters["iteration"]?.toLong() ?: error("Invalid status request")
            if (!printSystem.isCompleteFor(iteration)) engine.iterate()
            call.respond((printSystem.render(iteration) as Set<Any>).transformData())
        }

        addAdditionalRouting()
    }
}