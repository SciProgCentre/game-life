package space.kscience.simba.coroutines

import kotlinx.coroutines.CoroutineScope
import space.kscience.simba.engine.*
import space.kscience.simba.state.*
import space.kscience.simba.utils.Vector
import space.kscience.simba.utils.product
import space.kscience.simba.utils.toIndex
import space.kscience.simba.utils.toVector
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class CoroutinesActorEngine<State: ObjectState<State, Env>, Env: EnvironmentState>(
    private val dimensions: Vector,
    private val neighborsIndices: Set<Vector>,
    private val init: (Vector) -> State,
): Engine<Env>, CoroutineScope {
    private lateinit var field: List<Actor>

    override val coroutineContext: CoroutineContext
        get() = EmptyCoroutineContext

    override var started: Boolean = false
    override val systems: MutableList<EngineSystem> = mutableListOf()

    override fun init() {
        started = true

        fun cyclicMod(i: Int, n: Int): Int {
            return if (i >= 0) i % n else n + i % n
        }

        fun getNeighboursIds(v: Vector): List<Vector> {
            return neighborsIndices.map { neighbour ->
                v.zip(dimensions)
                    .mapIndexed { index, (position, dimensionBorder) -> cyclicMod(position - neighbour[index], dimensionBorder) }
                    .toIntArray()
            }
        }

        field = (0 until dimensions.product()).map { index ->
            val vector = index.toVector(dimensions)
            val state = init(vector)
            CoroutinesCellActor(this, coroutineContext).apply { this.handle(Init(vector, state)) }
        }

        field.forEachIndexed { index, actorRef ->
            getNeighboursIds(index.toVector(dimensions))
                .map { v -> field[v.toIndex(dimensions)] }
                .forEach { neighbour -> actorRef.handle(AddNeighbour(neighbour)) }
        }
    }

    override fun iterate() {
        field.forEach { it.handle(Iterate()) }
    }

    override fun setNewEnvironment(env: Env) {
        field.forEach { it.handle(UpdateEnvironment(env)) }
    }

    internal fun processActorMessage(msg: Message) {
        processWithSystems(msg)
    }
}