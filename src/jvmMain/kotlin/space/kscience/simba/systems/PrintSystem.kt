package space.kscience.simba.systems

import space.kscience.simba.ActorClassicCell
import space.kscience.simba.PassState
import space.kscience.simba.engine.EngineSystem
import space.kscience.simba.engine.Message
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PrintSystem(private val fieldSize: Int) : EngineSystem {
    private val statesByTimestamp = ConcurrentHashMap<Long, ConcurrentSkipListSet<ActorClassicCell>>()
    private val continuations = mutableListOf<Pair<Long, Continuation<Set<ActorClassicCell>>>>()

    override fun process(msg: Message) {
        if (msg is PassState) {
            statesByTimestamp
                .getOrPut(msg.timestamp) { ConcurrentSkipListSet() }
                .add(msg.state)

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

    suspend fun render(iteration: Long): Set<ActorClassicCell> {
        val states = statesByTimestamp[iteration]
        return if (states == null || states.size != fieldSize) {
            suspendCoroutine<Set<ActorClassicCell>> {
                continuations += iteration to it
            }
        } else {
            states
        }
    }
}