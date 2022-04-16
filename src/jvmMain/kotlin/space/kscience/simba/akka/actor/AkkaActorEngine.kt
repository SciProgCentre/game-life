package space.kscience.simba.akka.actor

import akka.actor.typed.ActorSystem
import space.kscience.simba.ActorCellEnvironmentState
import space.kscience.simba.ActorCellState
import space.kscience.simba.ActorClassicCell
import space.kscience.simba.engine.Actor
import space.kscience.simba.engine.Engine
import space.kscience.simba.engine.EngineSystem
import space.kscience.simba.engine.Message

class AkkaActorEngine(
    private val n: Int, private val m: Int,
    private val init: (Int, Int) -> ActorCellState,
    private val nextStep: (ActorCellState, ActorCellEnvironmentState) -> ActorCellState
) : Engine {
    var field: List<Actor<AkkaCellActor.Companion.CellActorMessage>>
//    val statesByTimestamp = mutableMapOf<Long, MutableList<ActorClassicCell>>()
//    private var timestamp = 0L
//    val renderingQueue = mutableListOf<MainActor.Companion.Render>()

    private val neighborsIndices = setOf(
        Pair(-1, -1), Pair(-1, 0), Pair(-1, 1),
        Pair(0, -1), Pair(0, 1),
        Pair(1, -1), Pair(1, 0), Pair(1, 1)
    )

    private val actorSystem = ActorSystem.create(MainActor.create(), "mainActor")
    override val systems: List<EngineSystem>
        get() = emptyList()

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
                spawn(CellActor(this, state, nextStep), "Actor_${i}_$j")
            }
        }.flatten()

        field.forEachIndexed { index, actor ->
            val i = index / m
            val j = index % m
            getNeighboursIds(i, j)
                .map { (k, l) -> field[k * n + l] }
                .forEach { neighbour -> actor.handleAndCallSystems(AkkaCellActor.Companion.AddNeighbour(neighbour)) }
        }
    }

    override fun <T : Message> spawn(actor: Actor<T>, name: String): Actor<T> {
        actorSystem.tell(MainActor.Companion.SpawnCell(actor as CellActor, name))
        return actor
    }

    override fun iterate() {
        field.forEach { it.handleAndCallSystems(AkkaCellActor.Companion.Iterate()) }
    }
}