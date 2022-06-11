package space.kscience.simba.akka.actor

import space.kscience.simba.akka.AkkaEngine
import space.kscience.simba.akka.SpawnCells
import space.kscience.simba.state.Cell
import space.kscience.simba.state.EnvironmentState
import space.kscience.simba.state.ObjectState
import space.kscience.simba.utils.Vector

class AkkaActorEngine<C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState>(
    private val dimensions: Vector,
    private val neighborsIndices: Set<Vector>,
    private val init: (Vector) -> C,
    private val nextStep: (State, Env) -> State
) : AkkaEngine() {
    override fun init() {
        actorSystem.tell(SpawnCells(dimensions, neighborsIndices) { index ->
            CellActor(this, init(index), nextStep)
        })
    }
}