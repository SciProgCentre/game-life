package space.kscience.simba.akka.actor

import akka.actor.typed.ActorSystem
import space.kscience.simba.ActorCellEnvironmentState
import space.kscience.simba.ActorCellState
import space.kscience.simba.engine.Engine
import space.kscience.simba.engine.EngineSystem
import space.kscience.simba.systems.PrintSystem

class AkkaActorEngine(
    n: Int, m: Int,
    init: (Int, Int) -> ActorCellState,
    nextStep: (ActorCellState, ActorCellEnvironmentState) -> ActorCellState
) : Engine {
    private val actorSystem = ActorSystem.create(MainActor.create(), "gameOfLife")
    override val systems: List<EngineSystem>
        get() = listOf(PrintSystem())

    init {
        actorSystem.tell(MainActor.Companion.SpawnCells(n, m, this, init, nextStep))
    }

    override fun iterate() {
        actorSystem.tell(MainActor.Companion.MainIterate())
    }
}