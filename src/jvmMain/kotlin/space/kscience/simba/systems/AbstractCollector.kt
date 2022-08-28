package space.kscience.simba.systems

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import space.kscience.simba.engine.EngineSystem
import space.kscience.simba.engine.Message
import space.kscience.simba.engine.PassState
import space.kscience.simba.state.Cell
import space.kscience.simba.state.ObjectState
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class AbstractCollector<C : Cell<C, State>, State : ObjectState> : EngineSystem, CoroutineScope {
    override val coroutineContext = Dispatchers.Unconfined

    private val statesByTimestamp = mutableMapOf<Long, MutableSet<C>>()
    private val continuations = mutableListOf<Pair<Long, Continuation<Set<C>>>>()
    private val lock = Object()

    private val channel = Channel<PassState<C, State>>()

    init {
        launch {
            for (msg in channel) {
                statesByTimestamp
                    .getOrPut(msg.timestamp) { mutableSetOf() }
                    .add(msg.state)

                if (isCompleteFor(msg.timestamp)) {
                    returnAvailable(msg.timestamp)
                }
            }
        }
    }

    abstract fun isCompleteFor(iteration: Long): Boolean

    @Suppress("UNCHECKED_CAST")
    override fun process(msg: Message) {
        if (msg is PassState<*, *>) {
            launch { channel.send(msg as PassState<C, State>) }
        }
    }

    @Synchronized
    private fun returnAvailable(iteration: Long) {
        synchronized(lock) {
            continuations
                .filter { it.first == iteration }
                .forEach { it.second.resume(statesByTimestamp[iteration]!!) }

            continuations.removeIf { it.first == iteration }
        }
    }

    protected fun tryToGetDataFor(iteration: Long): Set<C>? = statesByTimestamp[iteration]

    // TODO find a way to use only one `isCompleteFor` call
    suspend fun getDataFor(iteration: Long): Set<C> {
        if (isCompleteFor(iteration)) {
            return statesByTimestamp[iteration]!!
        }

        return suspendCoroutine<Set<C>> {
            if (isCompleteFor(iteration)) {
                it.resume(statesByTimestamp[iteration]!!)
            } else {
                synchronized(lock) { continuations += iteration to it }
            }

        }
    }
}