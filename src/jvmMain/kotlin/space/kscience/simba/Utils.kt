package space.kscience.simba

import akka.actor.typed.javadsl.*
import space.kscience.simba.engine.Message

interface ActorMessage<T: ActorMessage<T, E>, E: AbstractBehavior<T>>: Message {
    fun process(actor: E): E
}

fun <T: ActorMessage<T, E>, E: AbstractBehavior<T>, M: T> ReceiveBuilder<T>.onMessage(type: Class<M>, actor: E): ReceiveBuilder<T> {
    return this.onMessage(type) { it.process(actor) }!!
}

fun actorNextStep(state: ActorCellState, environmentState: ActorCellEnvironmentState): ActorCellState {
    val aliveNeighbours = environmentState.neighbours.count { it.isAlive() }
    if (state.isAlive) {
        if (aliveNeighbours != 2 && aliveNeighbours != 3) {
            return ActorCellState(false)
        }
    } else if (aliveNeighbours == 3) {
        return ActorCellState(true)
    }

    return state
}