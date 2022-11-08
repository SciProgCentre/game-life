package space.kscience.simba.akka.stream

import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import space.kscience.simba.akka.AkkaEngine
import space.kscience.simba.engine.AddNeighbour
import space.kscience.simba.engine.Init
import space.kscience.simba.engine.Iterate
import space.kscience.simba.state.Cell
import space.kscience.simba.state.ObjectState
import space.kscience.simba.utils.Vector
import space.kscience.simba.utils.product
import space.kscience.simba.utils.toIndex
import space.kscience.simba.utils.toVector
import java.util.concurrent.atomic.AtomicInteger

class AkkaStreamEngine<C: Cell<C, State>, State: ObjectState>(
    private val dimensions: Vector,
    private val neighborsIndices: Set<Vector>,
    private val init: (Vector) -> C,
) : AkkaEngine<Void>() {
    override val actorSystem: ActorSystem<Void> by lazy {
        ActorSystem.create(Behaviors.empty(), "AkkaSystem")
    }

    private lateinit var field: List<AkkaStreamActor<C, State>>
    private var initialTotalCountOfNeighbours = 0
    private var subscribedCount = AtomicInteger(0)

    override fun init() {
        field = (0 until dimensions.product()).map { index ->
            AkkaStreamActor(actorSystem, this).apply { this.handle(Init(init(index.toVector(dimensions)))) }
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

    fun subscribedToNeighbour(actor: AkkaStreamActor<C, State>) {
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
