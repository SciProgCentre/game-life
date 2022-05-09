package space.kscience.simba.systems

import space.kscience.simba.*
import space.kscience.simba.engine.EngineSystem
import space.kscience.simba.engine.Message
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PrintSystem<C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState>(private val fieldSize: Int) : EngineSystem {
    private val statesByTimestamp = ConcurrentHashMap<Long, ConcurrentSkipListSet<C>>()
    private val continuations = mutableListOf<Pair<Long, Continuation<Set<C>>>>()

    @Suppress("UNCHECKED_CAST")
    override fun process(msg: Message) {
        if (msg is PassState<*, *, *>) {
            statesByTimestamp
                .getOrPut(msg.timestamp) { ConcurrentSkipListSet() }
                .add(msg.state as C)

            if (statesByTimestamp[msg.timestamp]?.size == fieldSize) {
                renderAvailable(msg.timestamp)
            }
        }
    }

    @Synchronized
    private fun renderAvailable(iteration: Long) {
        continuations
            .filter { it.first == iteration }
            .forEach { it.second.resume(statesByTimestamp[it.first]!!) }

        continuations.removeIf { it.first == iteration }
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