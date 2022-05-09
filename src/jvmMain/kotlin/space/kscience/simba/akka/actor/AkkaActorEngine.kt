package space.kscience.simba.akka.actor

import akka.actor.typed.ActorSystem
import space.kscience.simba.*
import space.kscience.simba.engine.Engine
import space.kscience.simba.engine.EngineSystem

class AkkaActorEngine<C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState>(
    dimensions: Vector,
    init: (Vector) -> C,
    nextStep: (State, Env) -> State
) : Engine {
    private val actorSystem = ActorSystem.create(MainActor.create(), "gameOfLife")
    override val systems: MutableList<EngineSystem> = mutableListOf()

    init {
        actorSystem.tell(SpawnCells(dimensions, this, init, nextStep))
    }

    override fun iterate() {
        actorSystem.tell(SyncIterate())
    }
}