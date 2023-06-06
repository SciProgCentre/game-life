package space.kscience.simba.akka.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.ActorContext
import akka.cluster.sharding.typed.javadsl.ClusterSharding
import akka.cluster.sharding.typed.javadsl.EntityRef
import akka.cluster.sharding.typed.javadsl.EntityTypeKey
import akka.persistence.SnapshotOffer
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.RecoveryCompleted
import akka.persistence.typed.SnapshotCompleted
import akka.persistence.typed.javadsl.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import space.kscience.simba.akka.ActorInitialized
import space.kscience.simba.akka.ActorMessageForward
import space.kscience.simba.akka.MainActor
import space.kscience.simba.akka.MainActorMessage
import space.kscience.simba.engine.*
import space.kscience.simba.state.*
import kotlin.coroutines.CoroutineContext

// NB: when we recreate this actor, we also recreate reference on original `MainActor`.
// So all massages are forward to the original engine.
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

internal data class PersistentState<State : ObjectState<State, Env>, Env: EnvironmentState>(
    val cell: Cell<State, Env>,
    val timestamp: Long = -1L,
    val iterations: Int = 0,
    val neighbours: List<Actor> = mutableListOf(),
    val earlyStates: MutableMap<Long, MutableList<State>> = linkedMapOf(),
    val iterating: Boolean = false,
    val environment: Env? = null,
)

internal class EventAkkaActor<State : ObjectState<State, Env>, Env: EnvironmentState>(
    private val context: ActorContext<Message>,
    private val entityId: String,
    persistenceId: PersistenceId
): EventSourcedBehavior<Message, Message, PersistentState<State, Env>>(persistenceId), CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Unconfined
    private val log = context.system.log()
    private var inRecovery = true

    init {
        log.info(MainActor.logMarker, "Create akka Actor with ID $entityId")
    }

    // Inline is used here because we want to print correct method where `log.info` was called (see %M option in config)
    @Suppress("NOTHING_TO_INLINE")
    private inline fun info(msg: String, state: PersistentState<State, Env>?) {
        log.info(MainActor.logMarker, "[Actor $entityId] [Time ${state?.timestamp}] $msg")
    }

    override fun emptyState(): PersistentState<State, Env>? {
        // NB: this nullable state can be a problem because in every method we accept non-nullable state.
        // But it should be OK for as long as we process `Init` message as a first one.
        return null
    }

    override fun commandHandler(): CommandHandler<Message, Message, PersistentState<State, Env>> {
        return newCommandHandlerBuilder()
            .forAnyState()
            .onCommand(Init::class.java) { it ->
                Effect().persist(it).thenRun {
                    // `thenRun` is side effect, it would NOT be called if object was recreated
                    @Suppress("UNCHECKED_CAST")
                    (context.system as ActorSystem<MainActorMessage>).tell(ActorInitialized(entityId))
                }
            }
            .onAnyCommand(Effect()::persist)
    }

    override fun eventHandler(): EventHandler<PersistentState<State, Env>, Message> {
        return newEventHandlerBuilder()
            .forAnyState()
            .onEvent(Init::class.java) { state, msg -> onInitMessage(state, msg as Init<State, Env>) }
            .onEvent(AddNeighbour::class.java, ::onAddNeighbourMessage)
            .onEvent(Iterate::class.java, ::onIterateMessage)
            .onEvent(PassState::class.java) { state, msg -> onPassStateMessage(state, msg as PassState<State, Env>) }
            .onEvent(UpdateSelfState::class.java) { state, msg -> onUpdateSelfState(state, msg as UpdateSelfState<State, Env>) }
            .onEvent(UpdateEnvironment::class.java) { state, msg -> onUpdateEnvironment(state, msg as UpdateEnvironment<Env>) }
            .build()
    }

    override fun signalHandler(): SignalHandler<PersistentState<State, Env>> {
        return newSignalHandlerBuilder()
            .onSignal(RecoveryCompleted.instance()) { state ->
                inRecovery = false
                info("Recovery completed", state)
            }
            .build()
    }

    private fun onInitMessage(oldState: PersistentState<State, Env>?, msg: Init<State, Env>): PersistentState<State, Env> {
        // We can get second `Init` message when new node is connecting to cluster. We should just ignore it.
        if (oldState != null) return oldState
        info("Got `Init` message \"${msg.state}\"", null)
        return PersistentState(Cell(msg.index, msg.state))
    }

    private fun onAddNeighbourMessage(oldState: PersistentState<State, Env>, msg: AddNeighbour): PersistentState<State, Env> {
        val cellActor = msg.cellActor as CellActor
        info("Got request for new neighbour with id ${cellActor.entityId}", oldState)
        cellActor.unwrap(context)
        return oldState.copy(neighbours = oldState.neighbours + cellActor)
    }

    private fun onIterateMessage(oldState: PersistentState<State, Env>, msg: Iterate): PersistentState<State, Env> {
        info("Got new iterate request", oldState)
        val newState = oldState.copy(iterations = oldState.iterations + 1)
        if (oldState.iterations != 0) return newState
        return forceIteration(newState)
    }

    private fun forceIteration(oldState: PersistentState<State, Env>): PersistentState<State, Env> {
        info("Send current state to neighbours", oldState)

        // Note: we must advance timestamp right after iteration request and not after full iteration process.
        // If we do it after full iteration process, we can have a situation when
        // actor got all neighbours' messages, iterate, but never send his own state.
        val newTimestamp = oldState.timestamp + 1
        doIfRecovered { oldState.neighbours.forEach { it.handle(PassState(oldState.cell.state, newTimestamp)) } }
        val newState = oldState.copy(
            earlyStates = LinkedHashMap(oldState.earlyStates.filter { it.key != newTimestamp }),
            timestamp = newTimestamp
        )
        oldState.earlyStates.remove(newTimestamp)?.forEach {
            saveState(newState, PassState(it, newState.timestamp))
        }
        return tryToIterate(newState)
    }

    private fun onPassStateMessage(oldState: PersistentState<State, Env>, msg: PassState<State, Env>): PersistentState<State, Env> {
        info("Got new message \"$msg\"", oldState)
        saveState(oldState, msg)
        if (msg.timestamp != oldState.timestamp) return oldState
        return tryToIterate(oldState)
    }

    private fun saveState(oldState: PersistentState<State, Env>, msg: PassState<State, Env>) {
        if (msg.timestamp == oldState.timestamp) {
            oldState.cell.addNeighboursState(msg.state)
            return
        }

        oldState.earlyStates
            .getOrPut(msg.timestamp) { mutableListOf() }
            .add(msg.state)
    }

    private fun tryToIterate(oldState: PersistentState<State, Env>): PersistentState<State, Env> {
        if (!oldState.cell.isReadyForIteration(oldState.environment, oldState.neighbours.size)) return oldState

        info("Launch process to create new state", oldState)
        val newState = oldState.copy(iterating = true)
        launch {
            val newCell = newState.cell.iterate(newState.environment)
            // Note: in theory, if we could process `UpdateSelfState` and not persist it, we would be able to run
            // next command unconditionally. When recovery process begin, we will recalculate state and send new
            // `UpdateSelfState` message (not the stored one). But first of all, we don't have such option and,
            // second, we can't handle new incoming requests while in recovery state, so it will not work.
            doIfRecovered { context.self.tell(UpdateSelfState(newCell.state, oldState.timestamp)) }
            info("New state \"${newCell.state}\" was created", oldState)
        }
        return newState
    }

    private fun onUpdateSelfState(oldState: PersistentState<State, Env>, msg: UpdateSelfState<State, Env>): PersistentState<State, Env> {
        val newState = oldState.copy(
            cell = Cell(oldState.cell.vectorId, msg.newState), timestamp = oldState.timestamp, iterating = false, iterations = oldState.iterations - 1
        )
        info("State was updated from \"${oldState.cell.state}\" to \"${newState.cell.state}\"", oldState)
        if (newState.iterations != 0) return forceIteration(newState)
        return newState
    }

    private fun onUpdateEnvironment(oldState: PersistentState<State, Env>, msg: UpdateEnvironment<Env>): PersistentState<State, Env> {
        info("Environment was updated from \"${oldState.environment}\" to \"${msg.env}\"", oldState)
        return oldState.copy(environment = msg.env)
    }

    private inline fun doIfRecovered(action: () -> Unit) {
        if (inRecovery) return
        action()
    }
}
