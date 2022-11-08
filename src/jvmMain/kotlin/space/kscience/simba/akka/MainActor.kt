package space.kscience.simba.akka

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.receptionist.ServiceKey
import akka.cluster.sharding.typed.javadsl.ClusterSharding
import akka.cluster.sharding.typed.javadsl.EntityRef
import space.kscience.simba.akka.actor.CellActor
import space.kscience.simba.akka.actor.WrappedCellActor
import space.kscience.simba.engine.AddNeighbour
import space.kscience.simba.engine.Init
import space.kscience.simba.engine.Iterate
import space.kscience.simba.engine.Message
import space.kscience.simba.state.Cell
import space.kscience.simba.state.ObjectState
import space.kscience.simba.utils.Vector
import space.kscience.simba.utils.product
import space.kscience.simba.utils.toIndex
import space.kscience.simba.utils.toVector
import java.io.Serializable

sealed class MainActorMessage: Serializable
class SpawnCells<C: Cell<C, State>, State: ObjectState>(
    val dimensions: Vector,
    val neighborsIndices: Set<Vector>,
    val init: (Vector) -> C,
): MainActorMessage()
class ActorInitialized(): MainActorMessage()
object SyncIterate: MainActorMessage()
class ActorMessageForward(val content: Message): MainActorMessage()

class MainActor private constructor(
    context: ActorContext<MainActorMessage>,
    private val engine: AkkaEngine<MainActorMessage>
): AbstractBehavior<MainActorMessage>(context) {
    private lateinit var neighborsIndices: Set<Vector>
    private lateinit var dimensions: Vector

    private var initializedCount = 0

    override fun createReceive(): Receive<MainActorMessage> {
        return newReceiveBuilder()
            .onMessage(SpawnCells::class.java) { onSpawnCells(it as SpawnCells) }
            .onMessage(SyncIterate::class.java, ::onSyncIterate)
            .onMessage(ActorInitialized::class.java, ::onActorInitialized)
            .onMessage(ActorMessageForward::class.java, ::onActorMessageForward)
            .build()
    }

    fun String.createEntityRef(): EntityRef<Message> {
        return ClusterSharding.get(context.system).entityRefFor(CellActor.ENTITY_TYPE, this)
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

    private fun tellEachEntity(createMsg: (Int) -> Message) {
        (0 until dimensions.product()).map { index ->
            CellActor(context.self, index.toString().createEntityRef()).handleWithoutResendingToEngine(createMsg(index))
        }
    }

    private fun tellEachEntityWithList(createMsg: (Int) -> List<Message>) {
        (0 until dimensions.product()).map { index ->
            val actor = CellActor(context.self, index.toString().createEntityRef())
            createMsg(index).forEach { actor.handleWithoutResendingToEngine(it) }
        }
    }

    private fun <C: Cell<C, State>, State: ObjectState> onSpawnCells(msg: SpawnCells<C, State>): MainActor {
        neighborsIndices = msg.neighborsIndices
        dimensions = msg.dimensions

        tellEachEntity { index -> Init(msg.init(index.toVector(msg.dimensions))) }

        return this
    }

    private fun onSyncIterate(msg: SyncIterate): MainActor {
        context.log.info("Iterate on ${context.system.address()}")
        tellEachEntity { Iterate() }
        return this
    }

    private fun onActorInitialized(msg: ActorInitialized): MainActor {
        context.log.info("Initialized")
        if (++initializedCount == dimensions.product()) {
            context.log.info("Started")
            engine.start {
                tellEachEntityWithList { index ->
                    getNeighboursIds(index.toVector(dimensions))
                        .map { AddNeighbour(WrappedCellActor(context.self, it.toIndex(dimensions).toString())) }
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
        fun create(engine: AkkaEngine<MainActorMessage>): Behavior<MainActorMessage> {
            return Behaviors.setup {
                MainActor(it, engine)
            }
        }
    }
}
