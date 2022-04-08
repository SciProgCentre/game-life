package space.kscience.simba

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.*
import kotlinx.coroutines.runBlocking

interface ActorMessage<T: ActorMessage<T, E>, E: AbstractBehavior<T>> {
    fun process(actor: E): E
}

fun <T: ActorMessage<T, E>, E: AbstractBehavior<T>, M: T> ReceiveBuilder<T>.onMessage(type: Class<M>, actor: E): ReceiveBuilder<T> {
    return this.onMessage(type) { it.process(actor) }!!
}

typealias RenderFun = suspend (List<ActorClassicCell>?) -> Unit

class MainActor private constructor(
    context: ActorContext<MainActorMessage>,
    val n: Int, val m: Int
): AbstractBehavior<MainActor.Companion.MainActorMessage>(context) {
    lateinit var field: List<ActorRef<CellActor.Companion.CellActorMessage>>
    val statesByTimestamp = mutableMapOf<Long, MutableList<ActorClassicCell>>()
    private var timestamp = 0L
    val renderingQueue = mutableListOf<Render>()

    private val neighborsIndices = setOf(
        Pair(-1, -1), Pair(-1, 0), Pair(-1, 1),
        Pair(0, -1), Pair(0, 1),
        Pair(1, -1), Pair(1, 0), Pair(1, 1)
    )

    override fun createReceive(): Receive<MainActorMessage> {
        return newReceiveBuilder()
            .onMessage(SpawnCells::class.java, this)
            .onMessage(Iterate::class.java, this)
            .onMessage(SaveState::class.java, this)
            .onMessage(Render::class.java, this)
            .build()
    }

    companion object {
        interface MainActorMessage: ActorMessage<MainActorMessage, MainActor>

        class SpawnCells(
            val init: (Int, Int) -> ActorCellState,
            val nextStep: (ActorCellState, ActorCellEnvironmentState) -> ActorCellState
        ): MainActorMessage {
            override fun process(actor: MainActor): MainActor {
                fun cyclicMod(i: Int, n: Int): Int {
                    return if (i >= 0) i % n else n + i % n
                }

                fun getNeighboursIds(i: Int, j: Int): List<Pair<Int, Int>> {
                    return actor.neighborsIndices.map { cyclicMod(i - it.first, actor.n) to cyclicMod(j - it.second, actor.m) }
                }

                val tempField = List(actor.n) { i -> List(actor.m) { j -> ActorClassicCell(i, j, init(i, j)) } }

                actor.field = tempField.mapIndexed { i, list ->
                    list.mapIndexed { j, state ->
                        actor.context.spawn(CellActor.create(state, actor.context.self, nextStep), "Actor_${i}_$j")
                    }
                }.flatten()

                actor.field.forEachIndexed { index, actorRef ->
                    val i = index / actor.m
                    val j = index % actor.m
                    getNeighboursIds(i, j)
                        .map { (k, l) -> actor.field[k * actor.n + l] }
                        .forEach { neighbour -> actorRef.tell(CellActor.Companion.AddNeighbour(neighbour)) }
                }

                return actor
            }
        }

        class Iterate: MainActorMessage {
            override fun process(actor: MainActor): MainActor {
                actor.field.forEach { it.tell(CellActor.Companion.Iterate()) }
                return actor
            }
        }

        class SaveState(val state: ActorClassicCell, val timestamp: Long): MainActorMessage {
            override fun process(actor: MainActor): MainActor {
                actor.statesByTimestamp
                    .getOrPut(timestamp) { mutableListOf() }
                    .add(state)
                if (actor.renderingQueue.isNotEmpty()) {
                    actor.context.self.tell(actor.renderingQueue.first())
                }
                return actor
            }
        }

        class Render(
            private val iteration: Long = -1, private val returnIfResultIsNotReady: Boolean, val render: RenderFun
        ): MainActorMessage {
            override fun process(actor: MainActor): MainActor {
                val timestamp = iteration.takeIf { it != -1L } ?: actor.timestamp
                if (actor.statesByTimestamp[timestamp]?.size != actor.n * actor.m) {
                    // result IS NOT ready
                    if (!returnIfResultIsNotReady) {
                        actor.renderingQueue += this
                    } else {
                        runBlocking { render(null) }
                    }
                } else {
                    // result IS ready
                    if (returnIfResultIsNotReady) {
                        actor.renderingQueue.removeFirstOrNull()
                    }
                    runBlocking { render(actor.statesByTimestamp[timestamp]!!) }
                    if (iteration == -1L) {
                        actor.timestamp++
                    }
                }
                return actor
            }
        }

        fun create(n: Int, m: Int): Behavior<MainActorMessage> {
            return Behaviors.setup { MainActor(it, n, m) }
        }
    }
}

class CellActor private constructor(
    context: ActorContext<CellActorMessage>,
    val state: ActorClassicCell,
    val parent: ActorRef<MainActor.Companion.MainActorMessage>,
    val nextStep: (ActorCellState, ActorCellEnvironmentState) -> ActorCellState
): AbstractBehavior<CellActor.Companion.CellActorMessage>(context) {
    private var timestamp = 1L
    private val neighbours = mutableListOf<ActorRef<CellActorMessage>>()

    override fun createReceive(): Receive<CellActorMessage> {
        return newReceiveBuilder()
            .onMessage(AddNeighbour::class.java, this)
            .onMessage(Iterate::class.java, this)
            .onMessage(PassState::class.java, this)
            .build()
    }

    companion object {
        interface CellActorMessage: ActorMessage<CellActorMessage, CellActor>

        class AddNeighbour(val actorRef: ActorRef<CellActorMessage>): CellActorMessage {
            override fun process(actor: CellActor): CellActor {
                actor.neighbours.add(actorRef);
                return actor
            }
        }

        class Iterate: CellActorMessage {
            override fun process(actor: CellActor): CellActor {
                actor.neighbours.forEach { it.tell(PassState(actor.state, actor.timestamp)) }
                actor.timestamp++
                return actor
            }
        }

        class PassState(val state: ActorClassicCell, val timestamp: Long): CellActorMessage {
            override fun process(actor: CellActor): CellActor {
                actor.state.addNeighboursState(state)
                if (actor.state.isReadyForIteration(actor.neighbours.size)) {
                    actor.state.iterate(actor.nextStep)
                    actor.state.endIteration()
                    actor.parent.tell(MainActor.Companion.SaveState(actor.state, timestamp))
                }
                return actor
            }
        }

        fun create(
            state: ActorClassicCell,
            parent: ActorRef<MainActor.Companion.MainActorMessage>,
            nextStep: (ActorCellState, ActorCellEnvironmentState) -> ActorCellState
        ): Behavior<CellActorMessage> {
            parent.tell(MainActor.Companion.SaveState(state, 0))
            return Behaviors.setup { CellActor(it, state, parent, nextStep) }
        }
    }
}

fun actorNextStep(state: ActorCellState, environmentState: ActorCellEnvironmentState): ActorCellState {
    val aliveNeighbours = environmentState.neighbours.count { it.isAlive() }
    if (state.isAlive) {
        if (aliveNeighbours != 2 && aliveNeighbours != 3) {
            return ActorCellState(false)
        }
    } else if (aliveNeighbours == 3) {
        return ActorCellState(true)
    }

    return state
}