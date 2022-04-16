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
) : Actor<AkkaCellActor.Companion.CellActorMessage> {
    val akkaCellActor = AkkaCellActor.create(state, nextStep)
    lateinit var akkaCellActorRef: ActorRef<AkkaCellActor.Companion.CellActorMessage>

    override fun handle(msg: AkkaCellActor.Companion.CellActorMessage) {
        akkaCellActorRef.tell(msg)
    }
}

class AkkaCellActor private constructor(
    context: ActorContext<CellActorMessage>,
    val state: ActorClassicCell,
    val nextStep: (ActorCellState, ActorCellEnvironmentState) -> ActorCellState
): AbstractBehavior<AkkaCellActor.Companion.CellActorMessage>(context) {
    private var timestamp = 1L
    private val neighbours = mutableListOf<Actor<CellActorMessage>>()

    override fun createReceive(): Receive<CellActorMessage> {
        return newReceiveBuilder()
            .onMessage(AddNeighbour::class.java, this)
            .onMessage(Iterate::class.java, this)
            .onMessage(PassState::class.java, this)
            .build()
    }

    companion object {
        interface CellActorMessage: ActorMessage<CellActorMessage, AkkaCellActor>

        class AddNeighbour(val cellActor: Actor<CellActorMessage>): CellActorMessage {
            override fun process(actor: AkkaCellActor): AkkaCellActor {
                actor.neighbours.add(this.cellActor);
                return actor
            }
        }

        class Iterate: CellActorMessage {
            override fun process(actor: AkkaCellActor): AkkaCellActor {
                actor.neighbours.forEach { it.handleAndCallSystems(PassState(actor.state, actor.timestamp)) }
                actor.timestamp++
                return actor
            }
        }

        class PassState(val state: ActorClassicCell, val timestamp: Long): CellActorMessage {
            override fun process(actor: AkkaCellActor): AkkaCellActor {
                actor.state.addNeighboursState(state)
                if (actor.state.isReadyForIteration(actor.neighbours.size)) {
                    actor.state.iterate(actor.nextStep)
                    actor.state.endIteration()
                }
                return actor
            }
        }

        fun create(
            state: ActorClassicCell,
            nextStep: (ActorCellState, ActorCellEnvironmentState) -> ActorCellState
        ): Behavior<CellActorMessage> {
            return Behaviors.setup { AkkaCellActor(it, state, nextStep) }
        }
    }
}