package space.kscience.simba.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.launch
import space.kscience.simba.engine.*
import space.kscience.simba.simulation.iterationMap
import space.kscience.simba.state.Cell
import space.kscience.simba.state.ObjectState
import kotlin.coroutines.CoroutineContext

@OptIn(ObsoleteCoroutinesApi::class)
class CoroutinesCellActor<C: Cell<C, State>, State: ObjectState>(
    // actors in coroutines can't run on different machines, so it is safe to use ref to engine here
    private val engine: CoroutinesActorEngine<C, State>,
    override val coroutineContext: CoroutineContext,
    private val index: Int,
) : Actor, CoroutineScope {
    private val debug = false
    private inner class Log {
        inline fun debug(value: String, time: Long) {
            if (!debug) return
            println("[Actor $index] [Time $time] $value")
        }
    }

    private val log = Log()

    init {
        log.debug("Create coroutine Actor", 0)
    }

    private val actor = actor<Message> {
        var timestamp = -1L
        var iterations = 0
        val neighbours = mutableListOf<Actor>()
        val earlyStates = linkedMapOf<Long, MutableList<State>>()

        lateinit var internalState: C

        fun onInit(msg: Init<C, State>) {
            internalState = msg.state
        }

        fun onAddNeighbourMessage(msg: AddNeighbour) {
            neighbours.add(msg.cellActor)
        }

        fun forceIteration() {
            log.debug("Send current state to neighbours", timestamp)

            // Note: we must advance timestamp right after iteration request and not after full iteration process.
            // If we do it after full iteration process, we can have a situation when
            // actor got all neighbours' messages, iterate, but never send his own state.
            timestamp++
            neighbours.forEach { it.handle(PassState(internalState.state, timestamp)) }
            earlyStates.remove(timestamp)?.forEach {
                this@CoroutinesCellActor.handleWithoutResendingToEngine(PassState(it, timestamp))
            }
        }

        fun onIterateMessage(msg: Iterate) {
            if (iterations++ != 0) return
            forceIteration()
        }

        fun onPassStateMessage(msg: PassState<State>) {
            log.debug("Got new message \"$msg\"", timestamp)

            if (msg.timestamp != timestamp) {
                earlyStates
                    .getOrPut(msg.timestamp) { mutableListOf() }
                    .add(msg.state)
                return
            }

            internalState.addNeighboursState(msg.state)
            if (internalState.isReadyForIteration(neighbours.size)) {
                launch {
                    val newCell = internalState.iterate(iterationMap[internalState::class.java] as suspend (State, List<State>) -> State)
                    handleWithoutResendingToEngine(UpdateSelfState(newCell, timestamp))
                }
            }
        }

        fun onUpdateSelfState(msg: UpdateSelfState<C, State>) {
            log.debug("State was updated from \"${internalState.state}\" to \"${msg.newCell.state}\"", timestamp)

            internalState = msg.newCell

            if (iterations > 0) {
                iterations--
                if (iterations != 0) forceIteration()
            }
        }

        @Suppress("UNCHECKED_CAST")
        for (msg in channel) { // iterate over incoming messages
            when (msg) {
                is Init<*, *> -> onInit(msg as Init<C, State>)
                is AddNeighbour -> onAddNeighbourMessage(msg)
                is Iterate -> onIterateMessage(msg)
                is PassState<*> -> onPassStateMessage(msg as PassState<State>)
                is UpdateSelfState<*, *> -> onUpdateSelfState(msg as UpdateSelfState<C, State>)
            }
        }
    }

    override fun handleWithoutResendingToEngine(msg: Message) {
        launch { actor.send(msg) }
    }

    override fun sendToEngine(msg: Message) {
        engine.processActorMessage(msg)
    }
}
