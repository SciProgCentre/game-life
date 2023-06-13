package space.kscience.simba.akka.stream

import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import com.typesafe.config.ConfigFactory
import space.kscience.simba.akka.AkkaEngine
import space.kscience.simba.engine.AddNeighbour
import space.kscience.simba.engine.Init
import space.kscience.simba.engine.Iterate
import space.kscience.simba.engine.UpdateEnvironment
import space.kscience.simba.state.*
import space.kscience.simba.utils.Vector
import space.kscience.simba.utils.product
import space.kscience.simba.utils.toIndex
import space.kscience.simba.utils.toVector
import java.util.concurrent.atomic.AtomicInteger

class AkkaStreamEngine<State: ObjectState<State, Env>, Env: EnvironmentState>(
    override val dimensions: Vector,
    private val neighborsIndices: Set<Vector>,
    private val init: (Vector) -> State,
) : AkkaEngine<Void, Env>() {
    override val actorSystem: ActorSystem<Void> by lazy {
        // We parse here empty string as config to avoid accidental `application.conf` loading
        ActorSystem.create(Behaviors.empty(), "AkkaSystem", ConfigFactory.parseString(""))
    }

    private lateinit var field: List<AkkaStreamActor<State, Env>>
    private var initialTotalCountOfNeighbours = 0
    private var subscribedCount = AtomicInteger(0)

    override fun init() {
        field = (0 until dimensions.product()).map { index ->
            AkkaStreamActor(actorSystem, this).apply {
                val vector = index.toVector(dimensions)
                this.handle(Init(vector, init(vector))) }
        }

        initialTotalCountOfNeighbours = List(field.size) { index -> getNeighboursIds(index.toVector(dimensions)).size }.sum()

        field.forEachIndexed { index, actor ->
            getNeighboursIds(index.toVector(dimensions))
                .map { v -> field[v.toIndex(dimensions)] }
                .forEach { neighbour -> actor.handle(AddNeighbour(neighbour)) }
        }
    }

    override fun onIterate() {
        field.forEach { it.handle(Iterate()) }
    }

    override fun setNewEnvironment(env: Env) {
        field.forEach { it.handle(UpdateEnvironment(env)) }
    }

    fun subscribedToNeighbour(actor: AkkaStreamActor<State, Env>) {
        if (subscribedCount.incrementAndGet() == initialTotalCountOfNeighbours) {
            start {  }
        }
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
}
