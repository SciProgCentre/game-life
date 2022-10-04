package space.kscience.simba.akka

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import space.kscience.simba.engine.*
import space.kscience.simba.utils.Vector
import space.kscience.simba.utils.product
import space.kscience.simba.utils.toIndex
import space.kscience.simba.utils.toVector

sealed class MainActorMessage
class SpawnCells(
    val dimensions: Vector,
    val neighborsIndices: Set<Vector>,
    val spawnCell: (ActorRef<MainActorMessage>, Vector) -> Pair<AkkaActor, Behavior<Message>>
): MainActorMessage()
class ActorInitialized(val actorRef: Actor): MainActorMessage()
object SyncIterate: MainActorMessage()
class ActorMessageForward(val content: Message): MainActorMessage()

class MainActor private constructor(
    context: ActorContext<MainActorMessage>,
    private val engine: AkkaEngine
): AbstractBehavior<MainActorMessage>(context) {
    lateinit var field: List<AkkaActor>
    lateinit var neighborsIndices: Set<Vector>
    lateinit var dimensions: Vector

    private var initializedCount = 0

    override fun createReceive(): Receive<MainActorMessage> {
        return newReceiveBuilder()
            .onMessage(SpawnCells::class.java, ::onSpawnCells)
            .onMessage(SyncIterate::class.java, ::onSyncIterate)
            .onMessage(ActorInitialized::class.java, ::onActorInitialized)
            .onMessage(ActorMessageForward::class.java, ::onActorMessageForward)
            .build()
    }

    private fun cyclicMod(i: Int, n: Int): Int {
        return if (i >= 0) i % n else n + i % n
    }

    private fun getNeighboursIds(v: Vector): List<Vector> {
        return neighborsIndices.map { neighbour ->
            v.zip(dimensions)
                .mapIndexed { index, (position, dimensionBorder) -> cyclicMod(position - neighbour[index], dimensionBorder) }
                .toIntArray()
        }
    }

    private fun onSpawnCells(msg: SpawnCells): MainActor {
        neighborsIndices = msg.neighborsIndices
        dimensions = msg.dimensions

        field = (0 until msg.dimensions.product()).map { index ->
            val (actor, behaviour) = msg.spawnCell(context.self, index.toVector(msg.dimensions))
            actor.akkaActorRef = context.spawn(behaviour, "Actor_${index}")
            actor
        }

        return this
    }

    private fun onSyncIterate(msg: SyncIterate): MainActor {
        field.forEach { it.handle(Iterate()) }
        return this
    }

    private fun onActorInitialized(msg: ActorInitialized): MainActor {
        if (++initializedCount == field.size) {
            engine.start {
                field.forEachIndexed { index, actorRef ->
                    getNeighboursIds(index.toVector(dimensions))
                        .map { v -> field[v.toIndex(dimensions)] }
                        .forEach { neighbour -> actorRef.handle(AddNeighbour(neighbour)) }
                }
            }
        }
        return this
    }

    private fun onActorMessageForward(msg: ActorMessageForward): MainActor {
        engine.processWithSystems(msg.content)
        return this
    }

    companion object {
        fun create(engine: AkkaEngine): Behavior<MainActorMessage> {
            return Behaviors.setup { MainActor(it, engine) }
        }
    }
}
