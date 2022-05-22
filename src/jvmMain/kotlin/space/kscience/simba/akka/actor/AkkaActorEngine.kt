package space.kscience.simba.akka.actor

import akka.actor.typed.ActorSystem
import space.kscience.simba.engine.Engine
import space.kscience.simba.engine.EngineSystem
import space.kscience.simba.state.Cell
import space.kscience.simba.state.EnvironmentState
import space.kscience.simba.state.ObjectState
import space.kscience.simba.utils.Vector

class AkkaActorEngine<C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState>(
    dimensions: Vector,
    neighborsIndices: Set<Vector>,
    init: (Vector) -> C,
    nextStep: (State, Env) -> State
) : Engine {
    private val actorSystem = ActorSystem.create(MainActor.create(), "gameOfLife")
    override val systems: MutableList<EngineSystem> = mutableListOf()

    init {
        actorSystem.tell(SpawnCells(dimensions, this, neighborsIndices, init, nextStep))
    }

    override fun iterate() {
        actorSystem.tell(SyncIterate())
    }
}