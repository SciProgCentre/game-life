package space.kscience.simba.akka.stream

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.javadsl.*
import io.ktor.util.collections.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import space.kscience.simba.engine.*
import space.kscience.simba.simulation.iterationMap
import space.kscience.simba.state.Cell
import space.kscience.simba.state.EnvironmentState
import space.kscience.simba.state.ObjectState
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

// TODO fix memory leak in case when field is big (100 x 100)
/**
 * There are several ways to implement engine based on streams:
 * 1. Push-based. Each actor have a queue, neighbour subscribes to it. As soon as new data is accessible
 * actor will get it. There is a big problem with this approach in akka. There is no way to understand that a
 * certain actor was subscribed. This can lead to lost messages. To avoid this we need to send "test" message to be
 * sure that all neighbours are subscribed. This is quite complicated to implement.
 * 2. Pull-based. This is current implementation. Actor try to get data from neighbour and if it is not available
 * actor will suspend. The problem wit such approach is that we can wait for slow agent while there are already
 * some data to analyze.
 */
class AkkaStreamActor<C: Cell<C, State>, State: ObjectState, Env: EnvironmentState>(
    private val system: ActorSystem<Void>,
    private val engine: AkkaStreamEngine<C, State, Env>,
): Actor, CoroutineScope {
    private lateinit var state: C
    private var environment: Env? = null

    override val coroutineContext: CoroutineContext = Dispatchers.Unconfined
    private val log = system.log()

    private val queue: SourceQueue<Message>
    private val subscriptions: Source<Message, NotUsed>

    private var timestamp = -1L
    private var iterations = AtomicInteger(0)
    private val neighbours = mutableListOf<SinkQueueWithCancel<Message>>()

    private val futures = ConcurrentList<CompletableFuture<Void>>()

    private val lock = Object()

    override fun handleWithoutResendingToEngine(msg: Message) {
        queue.offer(msg)
    }

    override fun sendToEngine(msg: Message) {
        engine.systems.forEach { it.process(msg) }
    }

    init {
        log.debug("Create akka stream Actor")

        val sink = BroadcastHub.of(Message::class.java, 8)
        val source = Source.queue<Message>(8, OverflowStrategy.backpressure())
        val (queue, subscriptions) = source.toMat(sink, Keep.both()).run(system).let { it.first() to it.second() }

        this@AkkaStreamActor.queue = queue
        this@AkkaStreamActor.subscriptions = subscriptions

        this@AkkaStreamActor.subscriptions.runForeach({ msg ->
            // TODO ensure that subscription is active
            when (msg) {
                is Init<*, *> -> state = msg.state as C
                is AddNeighbour -> onAddNeighbourMessage(msg)
                is Iterate -> onIterateMessage(msg)
                is PassState<*> -> Unit
                is UpdateSelfState<*, *> -> onUpdateSelfState(msg as UpdateSelfState<C, State>)
                is UpdateEnvironment<*> -> onUpdateEnvironment(msg as UpdateEnvironment<Env>)
            }
        }, system)
    }

    @Suppress("UNCHECKED_CAST")
    private fun onAddNeighbourMessage(msg: AddNeighbour) {
        neighbours += (msg.cellActor as AkkaStreamActor<C, State, Env>).subscriptions.runWith(Sink.queue<Message>(), system)
        engine.subscribedToNeighbour(this)
    }

    private fun tryToIterate() {
        if (state.isReadyForIteration(neighbours.size)) {
            launch {
                val newCell = state.iterate(iterationMap[state::class.java] as suspend (State, List<State>) -> State)
                handleWithoutResendingToEngine(UpdateSelfState(newCell, timestamp))
            }
        }
    }

    private fun onIterateMessage(msg: Iterate) {
        if (iterations.getAndIncrement() != 0) return
        forceIteration()
    }

    private fun forceIteration() {
        // Note: we must advance timestamp right after iteration request and not after full iteration process.
        // If we do it after full iteration process, we can have a situation when
        // actor got all neighbours' messages, iterate, but never send his own state.
        timestamp++

        val passState = PassState(state.state, timestamp)
        handle(passState)

        neighbours.mapIndexed { i, it ->
            futures += pullMessage(it, i)
        }

        while (futures.isNotEmpty()) {
            futures.removeLast().join()
        }

        tryToIterate()
    }

    @Suppress("UNCHECKED_CAST")
    private fun pullMessage(neighbour: SinkQueueWithCancel<Message>, index: Int): CompletableFuture<Void> {
        return neighbour.pull().thenApply<PassState<State>?> { maybeMsg ->
            if (maybeMsg.isEmpty) return@thenApply null

            val msg = maybeMsg.get()
            if (msg !is PassState<*>) return@thenApply null
            msg as PassState<State>
        }.thenAccept { state ->
            if (state == null) {
                futures += pullMessage(neighbour, index)
            } else {
                synchronized(lock) {
                    this.state.addNeighboursState(state.state)
                }
            }
        }.toCompletableFuture()
    }

    private fun onUpdateSelfState(msg: UpdateSelfState<C, State>) {
        state = msg.newCell

        if (iterations.decrementAndGet() > 0) {
            forceIteration()
        }
    }

    private fun onUpdateEnvironment(msg: UpdateEnvironment<Env>) {
        environment = msg.env
    }
}
