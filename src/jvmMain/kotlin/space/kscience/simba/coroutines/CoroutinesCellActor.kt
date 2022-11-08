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
) : Actor, CoroutineScope {
    private val actor = actor<Message> {
        var timestamp = 0L
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
            if (msg.timestamp != timestamp) {
                earlyStates
                    .getOrPut(msg.timestamp) { mutableListOf() }
                    .add(msg.state)
                return
            }

            internalState.addNeighboursState(msg.state)
            if (internalState.isReadyForIteration(neighbours.size)) {
                launch {
                    handleWithoutResendingToEngine(
                        UpdateSelfState(
                            internalState.iterate(iterationMap[internalState::class.java] as suspend (State, List<State>) -> State),
                            timestamp
                        )
                    )
                }
            }
        }

        fun onUpdateSelfState(msg: UpdateSelfState<C, State>) {
            internalState = msg.newCell

            iterations--
            if (iterations > 0) {
                forceIteration()
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
