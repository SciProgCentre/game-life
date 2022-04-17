package space.kscience.simba.akka.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import space.kscience.simba.*
import space.kscience.simba.engine.Actor
import space.kscience.simba.engine.Engine

class CellActor(
    override val engine: Engine,
    state: ActorClassicCell,
    nextStep: (ActorCellState, ActorCellEnvironmentState) -> ActorCellState
) : Actor<GameOfLifeMessage> {
    val akkaCellActor = AkkaCellActor.create(state, nextStep)
    lateinit var akkaCellActorRef: ActorRef<GameOfLifeMessage>

    override fun handle(msg: GameOfLifeMessage) {
        akkaCellActorRef.tell(msg)
    }
}

class AkkaCellActor private constructor(
    context: ActorContext<GameOfLifeMessage>,
    private val state: ActorClassicCell,
    private val nextStep: (ActorCellState, ActorCellEnvironmentState) -> ActorCellState
): AbstractBehavior<GameOfLifeMessage>(context) {
    private var timestamp = 1L
    private val neighbours = mutableListOf<Actor<GameOfLifeMessage>>()

    override fun createReceive(): Receive<GameOfLifeMessage> {
        return newReceiveBuilder()
            .onMessage(AddNeighbour::class.java, ::onAddNeighbourMessage)
            .onMessage(Iterate::class.java, ::onIterateMessage)
            .onMessage(PassState::class.java, ::onPassStateMessage)
            .build()
    }

    private fun onAddNeighbourMessage(msg: AddNeighbour): Behavior<GameOfLifeMessage> {
        neighbours.add(msg.cellActor);
        return this
    }

    private fun onIterateMessage(msg: Iterate): Behavior<GameOfLifeMessage> {
        neighbours.forEach { it.handleAndCallSystems(PassState(state, timestamp)) }
        timestamp++
        return this
    }

    private fun onPassStateMessage(msg: PassState): Behavior<GameOfLifeMessage> {
        state.addNeighboursState(msg.state)
        if (state.isReadyForIteration(neighbours.size)) {
            state.iterate(nextStep)
            state.endIteration()
        }
        return this
    }

    companion object {
        fun create(
            state: ActorClassicCell,
            nextStep: (ActorCellState, ActorCellEnvironmentState) -> ActorCellState
        ): Behavior<GameOfLifeMessage> {
            return Behaviors.setup { AkkaCellActor(it, state, nextStep) }
        }
    }
}