package space.kscience.simba.akka.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.ActorContext
import akka.cluster.sharding.typed.javadsl.ClusterSharding
import akka.cluster.sharding.typed.javadsl.EntityRef
import akka.cluster.sharding.typed.javadsl.EntityTypeKey
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.javadsl.CommandHandler
import akka.persistence.typed.javadsl.EventHandler
import akka.persistence.typed.javadsl.EventSourcedBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import space.kscience.simba.akka.ActorInitialized
import space.kscience.simba.akka.ActorMessageForward
import space.kscience.simba.akka.MainActorMessage
import space.kscience.simba.engine.*
import space.kscience.simba.simulation.iterationMap
import space.kscience.simba.state.Cell
import space.kscience.simba.state.ObjectState
import space.kscience.simba.state.actorNextStep
import kotlin.coroutines.CoroutineContext

class WrappedCellActor(
    private val mainActorRef: ActorRef<MainActorMessage>,
    private val entityId: String
): Actor {
    override fun handleWithoutResendingToEngine(msg: Message) {
        TODO("Not yet implemented")
    }

    override fun sendToEngine(msg: Message) {
        TODO("Not yet implemented")
    }

    override fun handle(msg: Message) {
        TODO("Not yet implemented")
    }

    fun unwrap(context: ActorContext<Message>): CellActor {
        return CellActor(mainActorRef, ClusterSharding.get(context.system).entityRefFor(CellActor.ENTITY_TYPE, entityId))
    }
}

class CellActor(
    private val mainActorRef: ActorRef<MainActorMessage>,
    private val entityRef: EntityRef<Message>,
): Actor {
    companion object {
        val ENTITY_TYPE: EntityTypeKey<Message> = EntityTypeKey.create(Message::class.java, CellActor::class.java.simpleName)
    }

    override fun handleWithoutResendingToEngine(msg: Message) {
        entityRef.tell(msg)
    }

    override fun sendToEngine(msg: Message) {
        mainActorRef.tell(ActorMessageForward(msg))
    }
}

//TODO
//inline fun <T, reified U : T> ReceiveBuilder<T>.addHandler(noinline handler: (U) -> Behavior<T>): ReceiveBuilder<T> {
//    return onMessage(U::class.java, handler)
//}

internal class EventAkkaActor<C : Cell<C, State>, State : ObjectState>(
    private val context: ActorContext<Message>,
    entityId: String,
    persistenceId: PersistenceId
): EventSourcedBehavior<Message, Message, PersistentState<C, State>>(persistenceId), CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Unconfined
    private val log = context.system.log()

    init {
        log.info("ID $entityId")
    }

    override fun emptyState(): PersistentState<C, State>? {
        // TODO reconsider
        return null
    }

    override fun commandHandler(): CommandHandler<Message, Message, PersistentState<C, State>> {
        return newCommandHandlerBuilder()
            .forAnyState()
            .onCommand(Init::class.java) { it ->
                Effect().persist(it)
                    .thenRun { (context.system as ActorSystem<MainActorMessage>).tell(ActorInitialized()) }
                // `thenRun` is side effect, it would not be called if object was recreated
            }
//            .onCommand(UpdateSelfState::class.java) { _ ->
//                Effect().none()
//            }
            // TODO stash if iteration = true
            .onAnyCommand(Effect()::persist)
    }

    override fun eventHandler(): EventHandler<PersistentState<C, State>, Message> {
        return newEventHandlerBuilder()
            .forAnyState()
            .onEvent(Init::class.java) { _, msg -> onInitMessage(msg as Init<C, State>) }
            .onEvent(AddNeighbour::class.java, ::onAddNeighbourMessage)
            .onEvent(Iterate::class.java, ::onIterateMessage)
            .onEvent(PassState::class.java) { state, msg -> onPassStateMessage(state, msg as PassState<State>) }
            .onEvent(UpdateSelfState::class.java) { state, msg -> onUpdateSelfState(state, msg as UpdateSelfState<C, State>) }
            .build()
    }

    private fun onInitMessage(msg: Init<C, State>): PersistentState<C, State> {
        log.info("new init ${msg.state}")
        return PersistentState(msg.state)
    }

    private fun onAddNeighbourMessage(oldState: PersistentState<C, State>, msg: AddNeighbour): PersistentState<C, State> {
        log.info("new neighbour for ($oldState) at ${context.system.address()}")
        return oldState.copy(neighbours = oldState.neighbours + msg.cellActor)
    }

    private fun onIterateMessage(oldState: PersistentState<C, State>, msg: Iterate): PersistentState<C, State> {
        log.info("new iterate at ${context.system.address()}")
        if (oldState.iterations != 0) return oldState.copy(iterations = oldState.iterations + 1)
        return forceIteration(oldState)
    }

    private fun forceIteration(oldState: PersistentState<C, State>): PersistentState<C, State> {
        log.info("time ${oldState.timestamp} at ${context.system.address()}")

        oldState.neighbours.forEach { (it as WrappedCellActor).unwrap(context).handle(PassState(oldState.cell.state, oldState.timestamp)) }
        val newState = oldState.copy(earlyStates = LinkedHashMap(oldState.earlyStates.filter { it.key != oldState.timestamp }))
        oldState.earlyStates.remove(oldState.timestamp)?.forEach {
            saveState(newState, PassState(it, newState.timestamp))
        }
        return tryToIterate(newState)
    }

    private fun onPassStateMessage(oldState: PersistentState<C, State>, msg: PassState<State>): PersistentState<C, State> {
        saveState(oldState, msg)
        if (msg.timestamp != oldState.timestamp) return oldState
        return tryToIterate(oldState)
    }

    private fun saveState(oldState: PersistentState<C, State>, msg: PassState<State>) {
        log.info("got pass msg ($msg) at time ${oldState.timestamp} for (${oldState.cell.state}) at ${context.system.address()}")

        if (msg.timestamp == oldState.timestamp) {
            oldState.cell.addNeighboursState(msg.state)
            return
        }

        oldState.earlyStates
            .getOrPut(msg.timestamp) { mutableListOf() }
            .add(msg.state)
    }

    private fun tryToIterate(oldState: PersistentState<C, State>): PersistentState<C, State> {
        if (!oldState.cell.isReadyForIteration(oldState.neighbours.size)) return oldState

        log.info("iterating at time ${oldState.timestamp} for (${oldState.cell.state})")
        val newState = oldState.copy(iterating = true)
        launch {
            val newCell = newState.cell.iterate(iterationMap[oldState.cell::class.java] as suspend (State, List<State>) -> State)
            context.self.tell(UpdateSelfState(newCell, oldState.timestamp))
            newCell.let { /*if (it.i == 0 && it.j == 0) */log.info("new state for (${oldState.cell.state}) at ${context.system.address()}") }
        }
        return newState
    }

    private fun onUpdateSelfState(oldState: PersistentState<C, State>, msg: UpdateSelfState<C, State>): PersistentState<C, State> {
        if (oldState.timestamp != msg.timestamp) {
            // TODO document why we need this. It is necessary because on recovery this event will be created twice, but we need one only
            log.info("already updated")
            return oldState
        }

        val newState = oldState.copy(cell = msg.newCell, timestamp = oldState.timestamp + 1, iterating = false)
        log.info("update state; old (${oldState.cell.state}) new (${newState.cell.state})}")
        if (newState.iterations != 0) {
            return forceIteration(newState.copy(iterations =  newState.iterations - 1))
        }
        return newState
    }
}

internal data class PersistentState<C : Cell<C, State>, State : ObjectState>(
    val cell: C,
    val timestamp: Long = 0L,
    val iterations: Int = 0,
    val neighbours: List<Actor> = mutableListOf(),
    val earlyStates: MutableMap<Long, MutableList<State>> = linkedMapOf(),
    val iterating: Boolean = false
)
