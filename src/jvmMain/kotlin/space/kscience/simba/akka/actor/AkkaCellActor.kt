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
    private val state: ActorClassicCell,
    nextStep: (ActorCellState, ActorCellEnvironmentState) -> ActorCellState
) : Actor<GameOfLifeMessage> {
    val akkaCellActor = AkkaCellActor.create(state, nextStep)
    lateinit var akkaCellActorRef: ActorRef<GameOfLifeMessage>

    override fun handle(msg: GameOfLifeMessage) {
        akkaCellActorRef.tell(msg)
    }

    override fun toString(): String {
        return state.toString()
    }
}

class AkkaCellActor private constructor(
    context: ActorContext<GameOfLifeMessage>,
    private var state: ActorClassicCell,
    private val nextStep: (ActorCellState, ActorCellEnvironmentState) -> ActorCellState
): AbstractBehavior<GameOfLifeMessage>(context) {
    private var timestamp = 0L
    private var iterations = 0
    private val neighbours = mutableListOf<Actor<GameOfLifeMessage>>()
    private val earlyStates = linkedMapOf<Long, MutableList<ActorClassicCell>>()

    override fun createReceive(): Receive<GameOfLifeMessage> {
        return newReceiveBuilder()
            .onMessage(AddNeighbour::class.java, ::onAddNeighbourMessage)
            .onMessage(Iterate::class.java, ::onIterateMessage)
            .onMessage(PassState::class.java, ::onPassStateMessage)
            .build()
    }

    private fun onAddNeighbourMessage(msg: AddNeighbour): Behavior<GameOfLifeMessage> {
        neighbours.add(msg.cellActor)
        return this
    }

    private fun onIterateMessage(msg: Iterate): Behavior<GameOfLifeMessage> {
        if (iterations++ != 0) return this
        return forceIteration()
    }

    private fun forceIteration(): Behavior<GameOfLifeMessage> {
        timestamp++
        neighbours.forEach { it.handleAndCallSystems(PassState(state, timestamp)) }
        earlyStates.remove(timestamp)?.forEach {
            context.self.tell(PassState(it, timestamp))
        }
        return this
    }

    private fun onPassStateMessage(msg: PassState): Behavior<GameOfLifeMessage> {
        if (msg.timestamp != timestamp) {
            earlyStates
                .getOrPut(msg.timestamp) { mutableListOf() }
                .add(msg.state)
            return this
        }

        state.addNeighboursState(msg.state)
        if (state.isReadyForIteration(neighbours.size)) {
            state = state.iterate(nextStep)

            iterations--
            if (iterations > 0) {
                forceIteration()
            }
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