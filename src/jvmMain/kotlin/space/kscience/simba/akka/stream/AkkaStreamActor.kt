package space.kscience.simba.akka.stream

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.stream.BoundedSourceQueue
import akka.stream.SourceRef
import akka.stream.javadsl.Sink
import akka.stream.javadsl.Source
import akka.stream.javadsl.StreamRefs
import space.kscience.simba.akka.AkkaActor
import space.kscience.simba.engine.*
import space.kscience.simba.state.Cell
import space.kscience.simba.state.EnvironmentState
import space.kscience.simba.state.ObjectState

class StreamActor<C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState>(
    override val engine: Engine,
    state: C,
    nextStep: (State, Env) -> State,
    spawnAkkaActor: (Behavior<Message>) -> ActorRef<Message>
): AkkaActor(spawnAkkaActor) {
    private lateinit var queue: BoundedSourceQueue<PassState<C, State, Env>>
    private lateinit var sourceRef: SourceRef<PassState<C, State, Env>>

    override val akkaActor: Behavior<Message> = Behaviors.setup { AkkaStreamActor(it, state, nextStep) }

    private inner class AkkaStreamActor(
        context: ActorContext<Message>,
        private var state: C,
        private val nextStep: (State, Env) -> State
    ): AbstractBehavior<Message>(context) {
        private var timestamp = 0L
        private var iterations = 0
        private val earlyStates = linkedMapOf<Long, MutableList<C>>()
        private var neighboursCount = 0

        init {
            val queueDeclaration = Source.queue<PassState<C, State, Env>>(2000)
            val (preMat, source) = queueDeclaration.preMaterialize(context.system).let { it.first() to it.second() }
            queue = preMat
            sourceRef = source.runWith(StreamRefs.sourceRef(), context.system)
        }

        @Suppress("UNCHECKED_CAST")
        override fun createReceive(): Receive<Message> {
            return newReceiveBuilder()
                .onMessage(AddNeighbour::class.java, ::onAddNeighbourMessage)
                .onMessage(Iterate::class.java, ::onIterateMessage)
                .onMessage(PassState::class.java) { onPassStateMessage(it as PassState<C, State, Env>) }
                .build()
        }

        @Suppress("UNCHECKED_CAST")
        private fun onAddNeighbourMessage(msg: AddNeighbour): Behavior<Message> {
            neighboursCount++
            (msg.cellActor as StreamActor<C, State, Env>).sourceRef.source
                .to(Sink.foreach {
                    if (it.timestamp != timestamp) {
                        earlyStates
                            .getOrPut(it.timestamp) { mutableListOf() }
                            .add(it.state)
                    } else {
                        state.addNeighboursState(it.state)
                        if (state.isReadyForIteration(neighboursCount)) {
                            state = state.iterate(nextStep)

                            iterations--
                            if (iterations > 0) {
                                forceIteration()
                            }
                        }
                    }
                })
                .run(context.system)
            return this
        }

        private fun onIterateMessage(msg: Iterate): Behavior<Message> {
            if (iterations++ != 0) return this
            return forceIteration()
        }

        private fun forceIteration(): Behavior<Message> {
            timestamp++
            val passState = PassState(state, timestamp)
            queue.offer(passState)
            this@StreamActor.handleAndCallSystems(passState)

            earlyStates.remove(timestamp)?.forEach {
                state.addNeighboursState(it)
            }

            return this
        }

        private fun onPassStateMessage(msg: PassState<C, State, Env>): Behavior<Message> {
            // This message can be ignored, and it is used only to pass msg to systems
            return this
        }
    }
}
