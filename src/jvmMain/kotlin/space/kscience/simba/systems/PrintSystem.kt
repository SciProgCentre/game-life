package space.kscience.simba.systems

import space.kscience.simba.state.EnvironmentState
import space.kscience.simba.state.ObjectState

class PrintSystem<State: ObjectState<State, Env>, Env: EnvironmentState>(private val fieldSize: Int) : AbstractCollector<State, Env>() {
    override fun isCompleteFor(iteration: Long): Boolean {
        val states = tryToGetDataFor(iteration)
        return !(states == null || states.size != fieldSize)
    }

    suspend fun render(iteration: Long): Set<State> {
        return getDataFor(iteration)
    }
}