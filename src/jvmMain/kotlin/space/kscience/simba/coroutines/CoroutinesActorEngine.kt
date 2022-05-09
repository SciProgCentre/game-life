package space.kscience.simba.coroutines

import kotlinx.coroutines.CoroutineScope
import space.kscience.simba.*
import space.kscience.simba.engine.Actor
import space.kscience.simba.engine.Engine
import space.kscience.simba.engine.EngineSystem
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class CoroutinesActorEngine<C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState>(
    private val dimensions: Vector,
    private val init: (Vector) -> C,
    private val nextStep: (State, Env) -> State
): Engine, CoroutineScope {
    private var field: List<Actor<GameOfLifeMessage>>

    private val neighborsIndices = setOf<Vector>(
        intArrayOf(-1, -1), intArrayOf(-1, 0), intArrayOf(-1, 1),
        intArrayOf(0, -1), intArrayOf(0, 1),
        intArrayOf(1, -1), intArrayOf(1, 0), intArrayOf(1, 1)
    )

    override val coroutineContext: CoroutineContext
        get() = EmptyCoroutineContext

    override val systems: MutableList<EngineSystem> = mutableListOf()

    init {
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
            val state = init(index.toVector(dimensions))
            CoroutinesCellActor(this, coroutineContext, state, nextStep)
        }

        field.forEachIndexed { index, actorRef ->
            getNeighboursIds(index.toVector(dimensions))
                .map { v -> field[v.toIndex(dimensions)] }
                .forEach { neighbour -> actorRef.handleAndCallSystems(AddNeighbour(neighbour)) }
        }
    }

    override fun iterate() {
        field.forEach { it.handleAndCallSystems(Iterate()) }
    }
}