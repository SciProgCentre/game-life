package space.kscience.simba.akka.actor

import akka.actor.typed.ActorSystem
import space.kscience.simba.engine.Engine
import space.kscience.simba.engine.EngineSystem
import space.kscience.simba.state.Cell
import space.kscience.simba.state.EnvironmentState
import space.kscience.simba.state.ObjectState
import space.kscience.simba.utils.Vector

class AkkaActorEngine<C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState>(
    private val dimensions: Vector,
    private val neighborsIndices: Set<Vector>,
    private val init: (Vector) -> C,
    private val nextStep: (State, Env) -> State
) : Engine {
    private val actorSystem = ActorSystem.create(MainActor.create(), "gameOfLife")

    override var started: Boolean = false
    override val systems: MutableList<EngineSystem> = mutableListOf()

    override fun init() {
        started = true
        actorSystem.tell(SpawnCells(dimensions, this, neighborsIndices, init, nextStep))
    }

    override fun iterate() {
        actorSystem.tell(SyncIterate())
    }
}