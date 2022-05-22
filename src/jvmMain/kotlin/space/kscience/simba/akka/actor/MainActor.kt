package space.kscience.simba.akka.actor

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import space.kscience.simba.engine.*
import space.kscience.simba.state.Cell
import space.kscience.simba.state.EnvironmentState
import space.kscience.simba.state.ObjectState
import space.kscience.simba.utils.Vector
import space.kscience.simba.utils.product
import space.kscience.simba.utils.toIndex
import space.kscience.simba.utils.toVector

sealed class MainActorMessage
class SpawnCells<C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState>(
    val dimensions: Vector, val engine: Engine, val neighborsIndices: Set<Vector>, val init: (Vector) -> C, val nextStep: (State, Env) -> State
): MainActorMessage()
class SyncIterate: MainActorMessage()

class MainActor private constructor(
    context: ActorContext<MainActorMessage>
): AbstractBehavior<MainActorMessage>(context) {
    lateinit var field: List<Actor>

    override fun createReceive(): Receive<MainActorMessage> {
        return newReceiveBuilder()
            .onMessage(SpawnCells::class.java) { onSpawnCells(it) }
            .onMessage(SyncIterate::class.java, ::onSyncIterate)
            .build()
    }

    private fun <C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState> onSpawnCells(msg: SpawnCells<C, State, Env>): MainActor {
        fun cyclicMod(i: Int, n: Int): Int {
            return if (i >= 0) i % n else n + i % n
        }

        fun getNeighboursIds(v: Vector): List<Vector> {
            return msg.neighborsIndices.map { neighbour ->
                v.zip(msg.dimensions)
                    .mapIndexed { index, (position, dimensionBorder) -> cyclicMod(position - neighbour[index], dimensionBorder) }
                    .toIntArray()
            }
        }

        field = (0 until msg.dimensions.product()).map { index ->
            val state = msg.init(index.toVector(msg.dimensions))
            val cellActor = CellActor<C, State, Env>(msg.engine, state, msg.nextStep)
            cellActor.akkaCellActorRef = context.spawn(cellActor.akkaCellActor, "Actor_${index}")
            cellActor
        }

        field.forEachIndexed { index, actorRef ->
            getNeighboursIds(index.toVector(msg.dimensions))
                .map { v -> field[v.toIndex(msg.dimensions)] }
                .forEach { neighbour -> actorRef.handleAndCallSystems(AddNeighbour(neighbour)) }
        }

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
