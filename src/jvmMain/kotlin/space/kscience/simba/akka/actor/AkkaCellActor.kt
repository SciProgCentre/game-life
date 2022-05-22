package space.kscience.simba.akka.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import space.kscience.simba.engine.*
import space.kscience.simba.state.Cell
import space.kscience.simba.state.EnvironmentState
import space.kscience.simba.state.ObjectState

class CellActor<C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState>(
    override val engine: Engine,
    private val state: C,
    nextStep: (State, Env) -> State
) : Actor {
    val akkaCellActor = AkkaCellActor.create(state, nextStep)
    lateinit var akkaCellActorRef: ActorRef<Message>

    override fun handle(msg: Message) {
        akkaCellActorRef.tell(msg)
    }
}

class AkkaCellActor<C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState> private constructor(
    context: ActorContext<Message>,
    private var state: C,
    private val nextStep: (State, Env) -> State
): AbstractBehavior<Message>(context) {
    private var timestamp = 0L
    private var iterations = 0
    private val neighbours = mutableListOf<Actor>()
    private val earlyStates = linkedMapOf<Long, MutableList<C>>()

    override fun createReceive(): Receive<Message> {
        return newReceiveBuilder()
            .onMessage(AddNeighbour::class.java, ::onAddNeighbourMessage)
            .onMessage(Iterate::class.java, ::onIterateMessage)
            .onMessage(PassState::class.java, ::onPassStateMessage)
            .build()
    }

    private fun onAddNeighbourMessage(msg: AddNeighbour): Behavior<Message> {
        neighbours.add(msg.cellActor)
        return this
    }

    private fun onIterateMessage(msg: Iterate): Behavior<Message> {
        if (iterations++ != 0) return this
        return forceIteration()
    }

    private fun forceIteration(): Behavior<Message> {
        timestamp++
        neighbours.forEach { it.handleAndCallSystems(PassState(state, timestamp)) }
        earlyStates.remove(timestamp)?.forEach {
            context.self.tell(PassState(it, timestamp))
        }
        return this
    }

    @Suppress("UNCHECKED_CAST")
    private fun onPassStateMessage(msg: PassState<*, *, *>): Behavior<Message> {
        if (msg.timestamp != timestamp) {
            earlyStates
                .getOrPut(msg.timestamp) { mutableListOf() }
                .add(msg.state as C)
            return this
        }

        state.addNeighboursState(msg.state as C)
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
        fun <C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState> create(
            state: C, nextStep: (State, Env) -> State
        ): Behavior<Message> {
            return Behaviors.setup { AkkaCellActor(it, state, nextStep) }
        }
    }
}