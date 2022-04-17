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

fun actorsToString(field: List<ActorClassicCell>): String {
    val builder = StringBuilder()
    val n = field.maxOf { it.i } + 1
    val m = field.maxOf { it.j } + 1

    for (i in 0 until n) {
        for (j in 0 until m) {
            builder.append(if (field[i * n + j].isAlive()) "X" else "O")
        }
        builder.append("\n")
    }
    builder.append("\n")
    return builder.toString()
}
