package space.kscience.simba.systems

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import space.kscience.simba.engine.EngineSystem
import space.kscience.simba.engine.Message
import space.kscience.simba.engine.PassState
import space.kscience.simba.state.Cell
import space.kscience.simba.state.EnvironmentState
import space.kscience.simba.state.ObjectState
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PrintSystem<C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState>(private val fieldSize: Int) : EngineSystem, CoroutineScope {
    override val coroutineContext = Dispatchers.Unconfined

    private val statesByTimestamp = mutableMapOf<Long, MutableSet<C>>()
    private val continuations = mutableListOf<Pair<Long, Continuation<Set<C>>>>()

    private val channel = Channel<PassState<C, State, Env>>()

    init {
        launch {
            for (msg in channel) {
                statesByTimestamp
                    .getOrPut(msg.timestamp) { mutableSetOf() }
                    .add(msg.state)

                if (statesByTimestamp[msg.timestamp]?.size == fieldSize) {
                    renderAvailable(msg.timestamp)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun process(msg: Message) {
        if (msg is PassState<*, *, *>) {
            launch { channel.send(msg as PassState<C, State, Env>) }
        }
    }

    @Synchronized
    private fun renderAvailable(iteration: Long) {
        continuations
            .filter { it.first == iteration }
            .forEach { it.second.resume(statesByTimestamp[iteration]!!) }

        continuations.removeIf { it.first == iteration }
    }

    fun isCompleteFor(iteration: Long): Boolean {
        val states = statesByTimestamp[iteration]
        return !(states == null || states.size != fieldSize)
    }

    suspend fun render(iteration: Long): Set<C> {
        val states = statesByTimestamp[iteration]
        return if (states == null || states.size != fieldSize) {
            suspendCoroutine<Set<C>> {
                continuations += iteration to it
            }
        } else {
            states
        }
    }
}