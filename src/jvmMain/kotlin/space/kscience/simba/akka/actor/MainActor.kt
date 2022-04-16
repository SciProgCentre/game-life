package space.kscience.simba.akka.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import kotlinx.coroutines.runBlocking
import space.kscience.simba.*

class MainActor private constructor(
    context: ActorContext<MainActorMessage>
): AbstractBehavior<MainActor.Companion.MainActorMessage>(context) {
//    lateinit var field: List<ActorRef<CellActor.Companion.CellActorMessage>>
//    val statesByTimestamp = mutableMapOf<Long, MutableList<ActorClassicCell>>()
//    private var timestamp = 0L
//    val renderingQueue = mutableListOf<Render>()
//
//    private val neighborsIndices = setOf(
//        Pair(-1, -1), Pair(-1, 0), Pair(-1, 1),
//        Pair(0, -1), Pair(0, 1),
//        Pair(1, -1), Pair(1, 0), Pair(1, 1)
//    )

    override fun createReceive(): Receive<MainActorMessage> {
        return newReceiveBuilder()
//            .onMessage(SpawnCells::class.java, this)
//            .onMessage(Iterate::class.java, this)
//            .onMessage(SaveState::class.java, this)
//            .onMessage(Render::class.java, this)
            .build()
    }

    companion object {
        interface MainActorMessage: ActorMessage<MainActorMessage, MainActor>

        class SpawnCell(val cell: CellActor, val name: String): MainActorMessage {
            override fun process(actor: MainActor): MainActor {
                cell.akkaCellActorRef = actor.context.spawn(cell.akkaCellActor, name)
                return actor
            }
        }

//        class SpawnCells(
//            val n: Int, val m: Int,
//            val init: (Int, Int) -> ActorCellState,
//            val nextStep: (ActorCellState, ActorCellEnvironmentState) -> ActorCellState
//        ): MainActorMessage {
//            override fun process(actor: MainActor): MainActor {
//                fun cyclicMod(i: Int, n: Int): Int {
//                    return if (i >= 0) i % n else n + i % n
//                }
//
//                fun getNeighboursIds(i: Int, j: Int): List<Pair<Int, Int>> {
//                    return actor.neighborsIndices.map { cyclicMod(i - it.first, n) to cyclicMod(j - it.second, m) }
//                }
//
//                val tempField = List(n) { i -> List(m) { j -> ActorClassicCell(i, j, init(i, j)) } }
//
//                actor.field = tempField.mapIndexed { i, list ->
//                    list.mapIndexed { j, state ->
//                        actor.context.spawn(CellActor.create(state, actor.context.self, nextStep), "Actor_${i}_$j")
//                    }
//                }.flatten()
//
//                actor.field.forEachIndexed { index, actorRef ->
//                    val i = index / m
//                    val j = index % m
//                    getNeighboursIds(i, j)
//                        .map { (k, l) -> actor.field[k * n + l] }
//                        .forEach { neighbour -> actorRef.tell(CellActor.Companion.AddNeighbour(neighbour)) }
//                }
//
//                return actor
//            }
//        }

//        class Iterate: MainActorMessage {
//            override fun process(actor: MainActor): MainActor {
//                actor.field.forEach { it.tell(CellActor.Companion.Iterate()) }
//                return actor
//            }
//        }
//
//        class SaveState(val state: ActorClassicCell, val timestamp: Long): MainActorMessage {
//            override fun process(actor: MainActor): MainActor {
//                actor.statesByTimestamp
//                    .getOrPut(timestamp) { mutableListOf() }
//                    .add(state)
//                if (actor.renderingQueue.isNotEmpty()) {
//                    actor.context.self.tell(actor.renderingQueue.first())
//                }
//                return actor
//            }
//        }
//
//        class Render(
//            private val iteration: Long = -1, private val returnIfResultIsNotReady: Boolean, val render: RenderFun
//        ): MainActorMessage {
//            override fun process(actor: MainActor): MainActor {
//                val timestamp = iteration.takeIf { it != -1L } ?: actor.timestamp
//                if (actor.statesByTimestamp[timestamp]?.size != actor.field.size) {
//                    // result IS NOT ready
//                    if (!returnIfResultIsNotReady) {
//                        actor.renderingQueue += this
//                    } else {
//                        runBlocking { render(null) }
//                    }
//                } else {
//                    // result IS ready
//                    if (returnIfResultIsNotReady) {
//                        actor.renderingQueue.removeFirstOrNull()
//                    }
//                    runBlocking { render(actor.statesByTimestamp[timestamp]!!) }
//                    if (iteration == -1L) {
//                        actor.timestamp++
//                    }
//                }
//                return actor
//            }
//        }

        fun create(): Behavior<MainActorMessage> {
            return Behaviors.setup { MainActor(it) }
        }
    }
}