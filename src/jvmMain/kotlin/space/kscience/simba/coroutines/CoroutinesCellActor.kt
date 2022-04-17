package space.kscience.simba.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.launch
import space.kscience.simba.*
import space.kscience.simba.engine.Actor
import space.kscience.simba.engine.Engine
import kotlin.coroutines.CoroutineContext

@OptIn(ObsoleteCoroutinesApi::class)
class CoroutinesCellActor(
    override val engine: Engine,
    override val coroutineContext: CoroutineContext,
    private var state: ActorClassicCell,
    private val nextStep: (ActorCellState, ActorCellEnvironmentState) -> ActorCellState
) : Actor<GameOfLifeMessage>, CoroutineScope {
    private val actor = actor<GameOfLifeMessage> {
        var timestamp = 0L
        var iterations = 0
        val neighbours = mutableListOf<Actor<GameOfLifeMessage>>()
        val earlyStates = linkedMapOf<Long, MutableList<ActorClassicCell>>()

        var internalState = state.copy()

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

        fun onPassStateMessage(msg: PassState) {
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

        for (msg in channel) { // iterate over incoming messages
            when (msg) {
                is AddNeighbour -> onAddNeighbourMessage(msg)
                is Iterate -> onIterateMessage(msg)
                is PassState -> onPassStateMessage(msg)
            }
        }
    }

    override fun handle(msg: GameOfLifeMessage) {
        launch { actor.send(msg) }
    }
}
