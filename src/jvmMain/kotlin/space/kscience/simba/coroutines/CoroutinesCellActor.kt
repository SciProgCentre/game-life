package space.kscience.simba.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.launch
import space.kscience.simba.engine.*
import space.kscience.simba.state.Cell
import space.kscience.simba.state.EnvironmentState
import space.kscience.simba.state.ObjectState
import kotlin.coroutines.CoroutineContext

@OptIn(ObsoleteCoroutinesApi::class)
class CoroutinesCellActor<C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState>(
    override val engine: Engine,
    override val coroutineContext: CoroutineContext,
    private var state: C,
    private val nextStep: (State, Env) -> State
) : Actor, CoroutineScope {
    private val actor = actor<Message> {
        var timestamp = 0L
        var iterations = 0
        val neighbours = mutableListOf<Actor>()
        val earlyStates = linkedMapOf<Long, MutableList<C>>()

        var internalState = state

        fun onAddNeighbourMessage(msg: AddNeighbour) {
            neighbours.add(msg.cellActor)
        }

        fun forceIteration() {
            timestamp++
            neighbours.forEach { it.handleAndCallSystems(PassState(internalState, timestamp)) }
            earlyStates.remove(timestamp)?.forEach {
                this@CoroutinesCellActor.handle(PassState(it, timestamp))
            }
        }

        fun onIterateMessage(msg: Iterate) {
            if (iterations++ != 0) return
            forceIteration()
        }

        fun onPassStateMessage(msg: PassState<C, State, Env>) {
            if (msg.timestamp != timestamp) {
                earlyStates
                    .getOrPut(msg.timestamp) { mutableListOf() }
                    .add(msg.state)
                return
            }

            internalState.addNeighboursState(msg.state)
            if (internalState.isReadyForIteration(neighbours.size)) {
                internalState = internalState.iterate(nextStep)

                iterations--
                if (iterations > 0) {
                    forceIteration()
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        for (msg in channel) { // iterate over incoming messages
            when (msg) {
                is AddNeighbour -> onAddNeighbourMessage(msg)
                is Iterate -> onIterateMessage(msg)
                is PassState<*, *, *> -> onPassStateMessage(msg as PassState<C, State, Env>)
            }
        }
    }

    override fun handle(msg: Message) {
        launch { actor.send(msg) }
    }
}
