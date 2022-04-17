package space.kscience.simba.akka.actor

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import space.kscience.simba.*
import space.kscience.simba.engine.Actor
import space.kscience.simba.engine.Engine

class MainActor private constructor(
    context: ActorContext<MainActorMessage>
): AbstractBehavior<MainActor.Companion.MainActorMessage>(context) {
    lateinit var field: List<Actor<GameOfLifeMessage>>

    private val neighborsIndices = setOf(
        Pair(-1, -1), Pair(-1, 0), Pair(-1, 1),
        Pair(0, -1), Pair(0, 1),
        Pair(1, -1), Pair(1, 0), Pair(1, 1)
    )

    override fun createReceive(): Receive<MainActorMessage> {
        return newReceiveBuilder()
            .onMessage(SpawnCells::class.java, this)
            .onMessage(MainIterate::class.java, this)
            .build()
    }

    companion object {
        interface MainActorMessage: ActorMessage<MainActorMessage, MainActor>

        class SpawnCells(
            val n: Int, val m: Int,
            private val engine: Engine,
            val init: (Int, Int) -> ActorCellState,
            val nextStep: (ActorCellState, ActorCellEnvironmentState) -> ActorCellState
        ): MainActorMessage {
            override fun process(actor: MainActor): MainActor {
                fun cyclicMod(i: Int, n: Int): Int {
                    return if (i >= 0) i % n else n + i % n
                }

                fun getNeighboursIds(i: Int, j: Int): List<Pair<Int, Int>> {
                    return actor.neighborsIndices.map { cyclicMod(i - it.first, n) to cyclicMod(j - it.second, m) }
                }

                val tempField = List(n) { i -> List(m) { j -> ActorClassicCell(i, j, init(i, j)) } }

                actor.field = tempField.mapIndexed { i, list ->
                    list.mapIndexed { j, state ->
                        val cellActor = CellActor(engine, state, nextStep)
                        cellActor.akkaCellActorRef = actor.context.spawn(cellActor.akkaCellActor, "Actor_${i}_$j")
                        cellActor
                    }
                }.flatten()

                actor.field.forEachIndexed { index, actorRef ->
                    val i = index / m
                    val j = index % m
                    getNeighboursIds(i, j)
                        .map { (k, l) -> actor.field[k * n + l] }
                        .forEach { neighbour -> actorRef.handleAndCallSystems(AddNeighbour(neighbour)) }
                }

                return actor
            }
        }

        class MainIterate: MainActorMessage {
            override fun process(actor: MainActor): MainActor {
                actor.field.forEach { it.handleAndCallSystems(Iterate()) }
                return actor
            }
        }

        fun create(): Behavior<MainActorMessage> {
            return Behaviors.setup { MainActor(it) }
        }
    }
}