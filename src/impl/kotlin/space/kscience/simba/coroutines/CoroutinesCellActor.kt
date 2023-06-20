package space.kscience.simba.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.launch
import space.kscience.simba.engine.*
import space.kscience.simba.state.*
import space.kscience.simba.utils.Vector
import kotlin.coroutines.CoroutineContext

@OptIn(ObsoleteCoroutinesApi::class)
class CoroutinesCellActor<State : ObjectState<State, Env>, Env : EnvironmentState>(
    // actors in coroutines can't run on different machines, so it is safe to use ref to engine here
    private val engine: CoroutinesActorEngine<State, Env>,
    override val coroutineContext: CoroutineContext,
) : Actor, CoroutineScope {
    private val debug = false

    private inner class Log {
        inline fun debug(value: String, time: Long, index: Vector) {
            if (!debug) return
            println("[Actor $index] [Time $time] $value")
        }
    }

    private val log = Log()

    init {
        log.debug("Create coroutine Actor", 0, Vector(0))
    }

    private val actor = actor<Message> {
        var timestamp = -1L
        var iterations = 0
        val neighbours = mutableListOf<Actor>()
        val earlyStates = linkedMapOf<Long, MutableList<State>>()

        lateinit var internalState: Cell<State, Env>
        var env: Env? = null

        fun onInit(msg: Init<State, Env>) {
            internalState = Cell(msg.index, msg.state)
        }

        fun onAddNeighbourMessage(msg: AddNeighbour) {
            neighbours.add(msg.cellActor)
        }

        fun forceIteration() {
            log.debug("Send current state to neighbours", timestamp, internalState.vectorId)

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

        fun onPassStateMessage(msg: PassState<State, Env>) {
            log.debug("Got new message \"$msg\"", timestamp, internalState.vectorId)

            if (msg.timestamp != timestamp) {
                earlyStates
                    .getOrPut(msg.timestamp) { mutableListOf() }
                    .add(msg.state)
                return
            }

            internalState.addNeighboursState(msg.state)
            if (internalState.isReadyForIteration(env, neighbours.size)) {
                launch {
                    val newCell = internalState.iterate(env)
                    handleWithoutResendingToEngine(UpdateSelfState(newCell.state))
                }
            }
        }

        fun onUpdateSelfState(msg: UpdateSelfState<State, Env>) {
            log.debug("State was updated from \"${internalState.state}\" to \"${msg.newState}\"", timestamp, internalState.vectorId)

            internalState = Cell(internalState.vectorId, msg.newState)

            if (iterations > 0) {
                iterations--
                if (iterations != 0) forceIteration()
            }
        }

        fun onUpdateEnvironment(msg: UpdateEnvironment<Env>) {
            env = msg.env
        }

        @Suppress("UNCHECKED_CAST")
        for (msg in channel) { // iterate over incoming messages
            when (msg) {
                is Init<*, *> -> onInit(msg as Init<State, Env>)
                is AddNeighbour -> onAddNeighbourMessage(msg)
                is Iterate -> onIterateMessage(msg)
                is PassState<*, *> -> onPassStateMessage(msg as PassState<State, Env>)
                is UpdateSelfState<*, *> -> onUpdateSelfState(msg as UpdateSelfState<State, Env>)
                is UpdateEnvironment<*> -> onUpdateEnvironment(msg as UpdateEnvironment<Env>)
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
