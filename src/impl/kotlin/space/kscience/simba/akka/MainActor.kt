package space.kscience.simba.akka

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.cluster.sharding.typed.javadsl.ClusterSharding
import akka.cluster.sharding.typed.javadsl.EntityRef
import org.slf4j.MarkerFactory
import space.kscience.simba.akka.actor.CellActor
import space.kscience.simba.engine.*
import space.kscience.simba.state.*
import space.kscience.simba.utils.Vector
import space.kscience.simba.utils.product
import space.kscience.simba.utils.toIndex
import space.kscience.simba.utils.toVector
import java.io.Serializable

sealed class MainActorMessage: Serializable
class SpawnCells<State: ObjectState<State, Env>, Env: EnvironmentState>(
    val dimensions: Vector,
    val neighborsIndices: Set<Vector>,
    val init: (Vector) -> State,
): MainActorMessage()
class ActorInitialized(val id: String): MainActorMessage()
object SyncIterate: MainActorMessage()
class ActorMessageForward(val content: Message): MainActorMessage()
class PassNewEnvironment<Env: EnvironmentState>(val env: Env): MainActorMessage()

class MainActor<Env: EnvironmentState> private constructor(
    context: ActorContext<MainActorMessage>,
    private val engine: AkkaEngine<MainActorMessage, Env>
): AbstractBehavior<MainActorMessage>(context) {
    private val log = context.log
    private lateinit var neighborsIndices: Set<Vector>
    private lateinit var dimensions: Vector

    private var initializedCount = 0

    override fun createReceive(): Receive<MainActorMessage> {
        return newReceiveBuilder()
            .onMessage(SpawnCells::class.java) { onSpawnCells(it as SpawnCells) }
            .onMessage(SyncIterate::class.java, ::onSyncIterate)
            .onMessage(ActorInitialized::class.java, ::onActorInitialized)
            .onMessage(ActorMessageForward::class.java, ::onActorMessageForward)
            .onMessage(PassNewEnvironment::class.java, { onPassNewEnvironment(it as PassNewEnvironment<Env>) })
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
            index.toString().createEntityRef().tell(createMsg(index))
        }
    }

    private fun tellEachEntityWithList(createMsg: (Int) -> List<Message>) {
        (0 until dimensions.product()).map { index ->
            createMsg(index).forEach { index.toString().createEntityRef().tell(it) }
        }
    }

    // TODO simplify
    private fun <State: ObjectState<State, E>, E: EnvironmentState> onSpawnCells(msg: SpawnCells<State, E>): MainActor<Env> {
        neighborsIndices = msg.neighborsIndices
        dimensions = msg.dimensions

        tellEachEntity { index ->
            val vector = index.toVector(msg.dimensions)
            Init(vector, msg.init(vector))
        }

        return this
    }

    private fun onSyncIterate(msg: SyncIterate): MainActor<Env> {
        log.info(logMarker, "[Main] New iterate request")
        tellEachEntity { Iterate() }
        return this
    }

    private fun onActorInitialized(msg: ActorInitialized): MainActor<Env> {
        log.info(logMarker, "[Main] Initialized actor with ID ${msg.id}")
        if (++initializedCount == dimensions.product()) {
            log.info(logMarker, "[Main] All actors are initialized. Start engine.")
            engine.start {
                tellEachEntityWithList { index ->
                    getNeighboursIds(index.toVector(dimensions))
                        .map { AddNeighbour(CellActor(context.self, it.toIndex(dimensions).toString())) }
                }
            }
        }
        return this
    }

    private fun onActorMessageForward(msg: ActorMessageForward): MainActor<Env> {
        engine.processWithAggregators(msg.content)
        return this
    }

    private fun onPassNewEnvironment(msg: PassNewEnvironment<Env>): MainActor<Env> {
        tellEachEntity { UpdateEnvironment(msg.env) }
        return this
    }

    companion object {
        internal val logMarker = MarkerFactory.getMarker("actor")

        fun <Env: EnvironmentState> create(engine: AkkaEngine<MainActorMessage, Env>): Behavior<MainActorMessage> {
            return Behaviors.setup {
                MainActor(it, engine)
            }
        }
    }
}
