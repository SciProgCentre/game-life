package space.kscience.simba.akka.actor

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import space.kscience.simba.*
import space.kscience.simba.engine.Actor

class MainActor private constructor(
    context: ActorContext<MainActorMessage>
): AbstractBehavior<MainActorMessage>(context) {
    lateinit var field: List<Actor<GameOfLifeMessage>>

    private val neighborsIndices = setOf(
        Pair(-1, -1), Pair(-1, 0), Pair(-1, 1),
        Pair(0, -1), Pair(0, 1),
        Pair(1, -1), Pair(1, 0), Pair(1, 1)
    )

    override fun createReceive(): Receive<MainActorMessage> {
        return newReceiveBuilder()
            .onMessage(SpawnDiscreteCells::class.java) { onSpawnDiscreteCells(it) }
            .onMessage(SpawnContinuousCells::class.java) { onSpawnContinuousCells(it) }
            .onMessage(SyncIterate::class.java, ::onSyncIterate)
            .build()
    }

    private fun <C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState> onSpawnDiscreteCells(msg: SpawnDiscreteCells<C, State, Env>): MainActor {
        val (n, m) = msg.n to msg.m

        fun cyclicMod(i: Int, n: Int): Int {
            return if (i >= 0) i % n else n + i % n
        }

        fun getNeighboursIds(i: Int, j: Int): List<Pair<Int, Int>> {
            return neighborsIndices.map { cyclicMod(i - it.first, n) to cyclicMod(j - it.second, m) }
        }

        val tempField: List<List<C>> = List(n) { i -> List(m) { j -> msg.init(i, j) } }

        field = tempField.mapIndexed { i, list ->
            list.mapIndexed { j, state ->
                val cellActor = CellActor<C, State, Env>(msg.engine, state, msg.nextStep)
                cellActor.akkaCellActorRef = context.spawn(cellActor.akkaCellActor, "Actor_${i}_$j")
                cellActor
            }
        }.flatten()

        field.forEachIndexed { index, actorRef ->
            val i = index / m
            val j = index % m
            getNeighboursIds(i, j)
                .map { (k, l) -> field[k * n + l] }
                .forEach { neighbour -> actorRef.handleAndCallSystems(AddNeighbour(neighbour)) }
        }

        return this
    }

    private fun <C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState> onSpawnContinuousCells(msg: SpawnContinuousCells<C, State, Env>): MainActor {
        TODO("continuous not supported yet")
        return this
    }

    private fun onSyncIterate(msg: SyncIterate): MainActor {
        field.forEach { it.handleAndCallSystems(Iterate()) }
        return this
    }

    companion object {
        fun create(): Behavior<MainActorMessage> {
            return Behaviors.setup { MainActor(it) }
        }
    }
}