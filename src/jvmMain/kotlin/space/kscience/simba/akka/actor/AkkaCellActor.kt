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
import space.kscience.simba.akka.MainActor
import space.kscience.simba.akka.MainActorMessage
import space.kscience.simba.engine.*
import space.kscience.simba.simulation.iterationMap
import space.kscience.simba.state.Cell
import space.kscience.simba.state.ObjectState
import kotlin.coroutines.CoroutineContext

class CellActor(
    private val mainActorRef: ActorRef<MainActorMessage>,
    internal val entityId: String
): Actor {
    @Transient
    private lateinit var entityRef: EntityRef<Message>

    companion object {
        val ENTITY_TYPE: EntityTypeKey<Message> = EntityTypeKey.create(Message::class.java, CellActor::class.java.simpleName)
    }

    override fun handleWithoutResendingToEngine(msg: Message) {
        entityRef.tell(msg)
    }

    override fun sendToEngine(msg: Message) {
        mainActorRef.tell(ActorMessageForward(msg))
    }

    // This method is needed because we can't serialize `EntityRef`, so we must create it directly on agent
    fun unwrap(context: ActorContext<Message>): CellActor {
        entityRef = ClusterSharding.get(context.system).entityRefFor(ENTITY_TYPE, entityId)
        return this
    }
}

internal data class PersistentState<C : Cell<C, State>, State : ObjectState>(
    val cell: C,
    val timestamp: Long = -1L,
    val iterations: Int = 0,
    val neighbours: List<Actor> = mutableListOf(),
    val earlyStates: MutableMap<Long, MutableList<State>> = linkedMapOf(),
    val iterating: Boolean = false
)

internal class EventAkkaActor<C : Cell<C, State>, State : ObjectState>(
    private val context: ActorContext<Message>,
    private val entityId: String,
    persistenceId: PersistenceId
): EventSourcedBehavior<Message, Message, PersistentState<C, State>>(persistenceId), CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Unconfined
    private val log = context.system.log()

    init {
        log.info(MainActor.logMarker, "Create akka Actor with ID $entityId")
    }

    // Inline is used here because we want to print correct method where `log.info` was called (see %M option in config)
    @Suppress("NOTHING_TO_INLINE")
    private inline fun info(msg: String, state: PersistentState<C, State>?) {
        log.info(MainActor.logMarker, "[Actor $entityId] [Time ${state?.timestamp}] $msg")
    }

    override fun emptyState(): PersistentState<C, State>? {
        // NB: this nullable state can be a problem because in every method we accept non-nullable state.
        // But it should be OK for as long as we process `Init` message as a first one.
        return null
    }

    override fun commandHandler(): CommandHandler<Message, Message, PersistentState<C, State>> {
        return newCommandHandlerBuilder()
            .forAnyState()
            .onCommand(Init::class.java) { it ->
                Effect().persist(it).thenRun {
                    // `thenRun` is side effect, it would NOT be called if object was recreated
                    @Suppress("UNCHECKED_CAST")
                    (context.system as ActorSystem<MainActorMessage>).tell(ActorInitialized(entityId))
                }
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
        info("Got `Init` message \"${msg.state}\"", null)
        return PersistentState(msg.state)
    }

    private fun onAddNeighbourMessage(oldState: PersistentState<C, State>, msg: AddNeighbour): PersistentState<C, State> {
        val cellActor = msg.cellActor as CellActor
        info("Got request for new neighbour with id ${cellActor.entityId}", oldState)
        cellActor.unwrap(context)
        return oldState.copy(neighbours = oldState.neighbours + cellActor)
    }

    private fun onIterateMessage(oldState: PersistentState<C, State>, msg: Iterate): PersistentState<C, State> {
        info("Gor new iterate request", oldState)
        val newState = oldState.copy(iterations = oldState.iterations + 1)
        if (oldState.iterations != 0) return newState
        return forceIteration(newState)
    }

    private fun forceIteration(oldState: PersistentState<C, State>): PersistentState<C, State> {
        info("Send current state to neighbours", oldState)

        // Note: we must advance timestamp right after iteration request and not after full iteration process.
        // If we do it after full iteration process, we can have a situation when
        // actor got all neighbours' messages, iterate, but never send his own state.
        val newTimestamp = oldState.timestamp + 1
        oldState.neighbours.forEach { it.handle(PassState(oldState.cell.state, newTimestamp)) }
        val newState = oldState.copy(
            earlyStates = LinkedHashMap(oldState.earlyStates.filter { it.key != newTimestamp }),
            timestamp = newTimestamp
        )
        oldState.earlyStates.remove(newTimestamp)?.forEach {
            saveState(newState, PassState(it, newState.timestamp))
        }
        return tryToIterate(newState)
    }

    private fun onPassStateMessage(oldState: PersistentState<C, State>, msg: PassState<State>): PersistentState<C, State> {
        info("Got new message \"$msg\"", oldState)
        saveState(oldState, msg)
        if (msg.timestamp != oldState.timestamp) return oldState
        return tryToIterate(oldState)
    }

    private fun saveState(oldState: PersistentState<C, State>, msg: PassState<State>) {
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

        info("Launch process to create new state", oldState)
        val newState = oldState.copy(iterating = true)
        launch {
            val newCell = newState.cell.iterate(iterationMap[oldState.cell::class.java] as suspend (State, List<State>) -> State)
            context.self.tell(UpdateSelfState(newCell, oldState.timestamp))
            info("New state \"${newCell.state}\" was created", oldState)
        }
        return newState
    }

    private fun onUpdateSelfState(oldState: PersistentState<C, State>, msg: UpdateSelfState<C, State>): PersistentState<C, State> {
        if (oldState.timestamp != msg.timestamp) {
            // TODO document why we need this. It is necessary because on recovery this event will be created twice, but we need one only
            info("Update call was discarded because already updated", oldState)
            return oldState
        }

        val newState = oldState.copy(
            cell = msg.newCell, timestamp = oldState.timestamp, iterating = false, iterations = oldState.iterations - 1
        )
        info("State was updated from \"${oldState.cell.state}\" to \"${newState.cell.state}\"", oldState)
        if (newState.iterations != 0) return forceIteration(newState)
        return newState
    }
}
