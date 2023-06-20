package space.kscience.simba.aggregators

import space.kscience.simba.state.EnvironmentState
import space.kscience.simba.state.ObjectState

/**
 * Engine aggregator that listen to incoming messages of type `PassState` and groups them by iteration number.
 * Result of `i`-th iteration can be accessed using `render(i)`.
 */
class PrintAggregator<State : ObjectState<State, Env>, Env : EnvironmentState>(private val fieldSize: Int) : AbstractCollector<State, Env>() {
    override fun isCompleteFor(iteration: Long): Boolean {
        val states = tryToGetDataFor(iteration)
        return !(states == null || states.size != fieldSize)
    }

    /**
     * Return all `PassState` messages that were send during given iteration number.
     */
    suspend fun render(iteration: Long): Set<State> {
        return getDataFor(iteration)
    }
}