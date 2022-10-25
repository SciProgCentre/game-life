package space.kscience.simba.akka.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import space.kscience.simba.akka.ActorInitialized
import space.kscience.simba.akka.ActorMessageForward
import space.kscience.simba.akka.AkkaActor
import space.kscience.simba.akka.MainActorMessage
import space.kscience.simba.engine.*
import space.kscience.simba.state.Cell
import space.kscience.simba.state.ObjectState
import kotlin.coroutines.CoroutineContext

class CellActor(
    private val mainActor: ActorRef<MainActorMessage>,
) : AkkaActor() {
    private var wasCreated = false

    override fun <C : Cell<C, State>, State : ObjectState> create(
        state: C, nextState: suspend (State, List<C>) -> State
    ): Behavior<Message> {
        if (wasCreated) {
            throw AssertionError("CellActor can be created only once")
        }
        wasCreated = true
        return Behaviors.setup { AkkaCellActor(it, this, state, nextState) }
    }

    override fun handleWithoutResendingToEngine(msg: Message) {
        akkaActorRef.tell(msg)
    }

    override fun sendToEngine(msg: Message) {
        mainActor.tell(ActorMessageForward(msg))
    }
}

private class AkkaCellActor<C : Cell<C, State>, State : ObjectState>(
    context: ActorContext<Message>,
    actor: CellActor,
    private var state: C,
    private val nextState: suspend (State, List<C>) -> State,
) : AbstractBehavior<Message>(context), CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Unconfined

    private var timestamp = 0L
    private var iterations = 0
    private val neighbours = mutableListOf<Actor>()
    private val earlyStates = linkedMapOf<Long, MutableList<C>>()

    init {
        (context.system as ActorSystem<MainActorMessage>).tell(ActorInitialized(actor))
    }

    @Suppress("UNCHECKED_CAST")
    override fun createReceive(): Receive<Message> {
        return newReceiveBuilder()
            .onMessage(AddNeighbour::class.java, ::onAddNeighbourMessage)
            .onMessage(Iterate::class.java, ::onIterateMessage)
            .onMessage(PassState::class.java) { onPassStateMessage(it as PassState<C, State>) }
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
        neighbours.forEach { it.handle(PassState(state, timestamp)) }
        earlyStates.remove(timestamp)?.forEach {
            onPassStateMessage(PassState(it, timestamp))
        }
        return this
    }

    private fun onPassStateMessage(msg: PassState<C, State>): Behavior<Message> {
        if (msg.timestamp != timestamp) {
            earlyStates
                .getOrPut(msg.timestamp) { mutableListOf() }
                .add(msg.state)
            return this
        }

        state.addNeighboursState(msg.state)
        if (state.isReadyForIteration(neighbours.size)) {
            launch {
                state = state.iterate(nextState)

                if (--iterations > 0) {
                    forceIteration()
                }
            }
        }
        return this
    }
}