package space.kscience.simba.systems

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
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

    private val flow = MutableSharedFlow<PassState<C, State, Env>>(
        replay = fieldSize, extraBufferCapacity = fieldSize, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        launch {
            flow.collect {
                statesByTimestamp
                    .getOrPut(it.timestamp) { mutableSetOf() }
                    .add(it.state)

                if (statesByTimestamp[it.timestamp]?.size == fieldSize) {
                    renderAvailable(it.timestamp)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun process(msg: Message) {
        if (msg is PassState<*, *, *>) {
            launch { flow.emit(msg as PassState<C, State, Env>) }
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