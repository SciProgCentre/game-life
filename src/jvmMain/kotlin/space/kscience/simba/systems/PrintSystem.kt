package space.kscience.simba.systems

import space.kscience.simba.state.Cell
import space.kscience.simba.state.ObjectState

class PrintSystem<C: Cell<C, State>, State: ObjectState>(private val fieldSize: Int) : AbstractCollector<C, State>() {
    override fun isCompleteFor(iteration: Long): Boolean {
        val states = tryToGetDataFor(iteration)
        return !(states == null || states.size != fieldSize)
    }

    suspend fun render(iteration: Long): Set<C> {
        return getDataFor(iteration)
    }
}