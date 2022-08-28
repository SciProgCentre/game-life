package space.kscience.simba.akka.stream

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.stream.OverflowStrategy
import akka.stream.SourceRef
import akka.stream.javadsl.*
import io.ktor.util.collections.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import space.kscience.simba.akka.ActorInitialized
import space.kscience.simba.akka.AkkaActor
import space.kscience.simba.akka.MainActorMessage
import space.kscience.simba.engine.*
import space.kscience.simba.state.Cell
import space.kscience.simba.state.ObjectState
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

// TODO fix memory leak in case when field is big (100 x 100)
// TODO get rid of synchronized blocks
class StreamActor<C: Cell<C, State>, State: ObjectState>(
    override val engine: Engine,
    state: C,
    nextState: suspend (State, List<C>) -> State,
): AkkaActor(), CoroutineScope {
    private lateinit var queue: SourceQueue<PassState<C, State>>
    private lateinit var subscriptions: Source<PassState<C, State>, NotUsed>

    private var subscribed = AtomicInteger(0)
    override val akkaActor: Behavior<Message> = Behaviors.setup { AkkaStreamActor(it, state, nextState) }

    override val coroutineContext: CoroutineContext = Dispatchers.Unconfined

    fun createNewSubscriber(context: ActorContext<Message>): SourceRef<PassState<C, State>> {
        return (subscriptions.runWith(StreamRefs.sourceRef(), context.system) as SourceRef<PassState<C, State>>)
    }

    private inner class AkkaStreamActor(
        context: ActorContext<Message>,
        private var state: C,
        private val nextState: suspend (State, List<C>) -> State,
    ): AbstractBehavior<Message>(context) {
        private var timestamp = 0L
        private var iterations = AtomicInteger(0)
        private val earlyStates = mutableMapOf<Long, MutableList<C>>()
        private var neighboursCount = 0

        private val lock = Object() // we use this lock to avoid time change in the middle of processing new message

        private var wasSubscribed = mutableMapOf<C, Boolean>()

        init {
            val sink = BroadcastHub.of(PassState::class.java, 8)
            val source = Source.queue<PassState<*, *>>(8, OverflowStrategy.dropTail())
            val (queue, subscriptions) = source.toMat(sink, Keep.both()).run(context.system).let { it.first() to it.second() }

            this@StreamActor.queue = queue as SourceQueue<PassState<C, State>>
            this@StreamActor.subscriptions = subscriptions as Source<PassState<C, State>, NotUsed>

            (context.system as ActorSystem<MainActorMessage>).tell(ActorInitialized(this@StreamActor))
        }

        @Suppress("UNCHECKED_CAST")
        override fun createReceive(): Receive<Message> {
            return newReceiveBuilder()
                .onMessage(AddNeighbour::class.java, ::onAddNeighbourMessage)
                .onMessage(Iterate::class.java, ::onIterateMessage)
                .onMessage(PassState::class.java) { onPassStateMessage(it as PassState<C, State>) }
                .build()
        }

        @Suppress("UNCHECKED_CAST")
        private fun onAddNeighbourMessage(msg: AddNeighbour): Behavior<Message> {
            neighboursCount++
            (msg.cellActor as StreamActor<C, State>).createNewSubscriber(context).source.runForeach({
                if (it.timestamp == -1L) {
                    // notify producer that new subscriber arrived
                    if (wasSubscribed[it.state] != true) msg.cellActor.subscribed.incrementAndGet()
                    wasSubscribed[it.state] = true
                    return@runForeach
                }

                synchronized(lock) {
                    if (it.timestamp != timestamp) {
                        earlyStates
                            .getOrPut(it.timestamp) { mutableListOf() }
                            .add(it.state)
                    } else {
                        addStateAndTryToIterate(it.state)
                    }
                }
            }, context.system)
            return this
        }

        private fun addStateAndTryToIterate(neighbour: C) {
            state.addNeighboursState(neighbour)

            if (state.isReadyForIteration(neighboursCount)) {
                launch {
                    state = state.iterate(nextState)

                    if (iterations.decrementAndGet() > 0) {
                        forceIteration()
                    }
                }
            }
        }

        private fun onIterateMessage(msg: Iterate): Behavior<Message> {
            if (iterations.getAndIncrement() != 0) return this
            if (subscribed.get() != neighboursCount) {
                context.self.tell(PassState(state, -1))
                return this
            }
            return forceIteration()
        }

        private fun forceIteration(): Behavior<Message> {
            synchronized(lock) {
                val passState = PassState(state, ++timestamp)
                queue.offer(passState)
                this@StreamActor.handleAndCallSystems(passState)

                earlyStates.remove(timestamp)?.forEach { addStateAndTryToIterate(it) }
            }
            return this
        }

        private fun onPassStateMessage(msg: PassState<C, State>): Behavior<Message> {
            if (msg.timestamp == -1L) {
                // this is first message, it is used to ensure that all neighbours are subscribed
                // in other case they can miss a message
                if (subscribed.get() != neighboursCount) {
                    queue.offer(msg).thenRun {
                        // this resending can spam a lot of messages, so we use `OverflowStrategy.dropTail` strategy
                        context.system
                            .scheduler()
                            .scheduleOnce(Duration.ofSeconds(1L), { context.self.tell(msg) }, context.executionContext)
                    }
                } else {
                    forceIteration()
                }
            }
            // In other cases this message can be ignored, and it is used only to pass msg to systems
            return this
        }
    }
}
