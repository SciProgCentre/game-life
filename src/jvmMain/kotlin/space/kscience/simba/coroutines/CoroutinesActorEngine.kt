package space.kscience.simba.coroutines

import kotlinx.coroutines.CoroutineScope
import space.kscience.simba.*
import space.kscience.simba.engine.Actor
import space.kscience.simba.engine.Engine
import space.kscience.simba.engine.EngineSystem
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class CoroutinesActorEngine(
    private val n: Int, private val m: Int,
    private val init: (Int, Int) -> ActorCellState,
    private val nextStep: (ActorCellState, ActorCellEnvironmentState) -> ActorCellState
): Engine, CoroutineScope {
    private var field: List<Actor<GameOfLifeMessage>>

    private val neighborsIndices = setOf(
        Pair(-1, -1), Pair(-1, 0), Pair(-1, 1),
        Pair(0, -1), Pair(0, 1),
        Pair(1, -1), Pair(1, 0), Pair(1, 1)
    )

    override val coroutineContext: CoroutineContext
        get() = EmptyCoroutineContext

    override val systems: MutableList<EngineSystem> = mutableListOf()

    init {
        fun cyclicMod(i: Int, n: Int): Int {
            return if (i >= 0) i % n else n + i % n
        }

        fun getNeighboursIds(i: Int, j: Int): List<Pair<Int, Int>> {
            return neighborsIndices.map { cyclicMod(i - it.first, n) to cyclicMod(j - it.second, m) }
        }

        val tempField = List(n) { i -> List(m) { j -> ActorClassicCell(i, j, init(i, j)) } }

        field = tempField.mapIndexed { i, list ->
            list.mapIndexed { j, state ->
                CoroutinesCellActor(this, coroutineContext, state, nextStep)
            }
        }.flatten()

        field.forEachIndexed { index, actor ->
            val i = index / m
            val j = index % m
            getNeighboursIds(i, j)
                .map { (k, l) -> field[k * n + l] }
                .forEach { neighbour -> actor.handleAndCallSystems(AddNeighbour(neighbour)) }
        }
    }

    override fun iterate() {
        field.forEach { it.handleAndCallSystems(Iterate()) }
    }
}