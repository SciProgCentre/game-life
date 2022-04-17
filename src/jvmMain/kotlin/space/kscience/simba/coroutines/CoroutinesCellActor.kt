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
    private val state: ActorClassicCell,
    private val nextStep: (ActorCellState, ActorCellEnvironmentState) -> ActorCellState
) : Actor<GameOfLifeMessage>, CoroutineScope {
    private var timestamp = 1L
    private val neighbours = mutableListOf<Actor<GameOfLifeMessage>>()

    private val actor = actor<GameOfLifeMessage> {
        for (msg in channel) { // iterate over incoming messages
            when (msg) {
                is AddNeighbour -> neighbours.add(msg.cellActor)
                is Iterate -> {
                    neighbours.forEach { it.handleAndCallSystems(PassState(state, timestamp)) }
                    timestamp++
                }
                is PassState -> {
                    state.addNeighboursState(msg.state)
                    if (state.isReadyForIteration(neighbours.size)) {
                        state.iterate(nextStep)
                        state.endIteration()
                    }
                }
            }
        }
    }

    override fun handle(msg: GameOfLifeMessage) {
        launch { actor.send(msg) }
    }
}
