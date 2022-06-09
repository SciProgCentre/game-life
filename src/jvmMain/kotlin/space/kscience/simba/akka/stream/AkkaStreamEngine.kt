package space.kscience.simba.akka.stream

import akka.actor.typed.ActorSystem
import space.kscience.simba.akka.MainActor
import space.kscience.simba.akka.SpawnCells
import space.kscience.simba.akka.SyncIterate
import space.kscience.simba.engine.Engine
import space.kscience.simba.engine.EngineSystem
import space.kscience.simba.state.Cell
import space.kscience.simba.state.EnvironmentState
import space.kscience.simba.state.ObjectState
import space.kscience.simba.utils.Vector

class AkkaStreamEngine<C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState>(
    private val dimensions: Vector,
    private val neighborsIndices: Set<Vector>,
    private val init: (Vector) -> C,
    private val nextStep: (State, Env) -> State
) : Engine {
    private val actorSystem = ActorSystem.create(MainActor.create(), "AkkaStreamSystem")

    override var started: Boolean = false
    override val systems: MutableList<EngineSystem> = mutableListOf()

    override fun init() {
        started = true
        actorSystem.tell(SpawnCells(dimensions, neighborsIndices, init) { state, spawnAkkaActor ->
            StreamActor(this, state, nextStep, spawnAkkaActor)
        })
    }

    override fun iterate() {
        actorSystem.tell(SyncIterate)
    }
}