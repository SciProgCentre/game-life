package space.kscience.simba.akka.stream

import space.kscience.simba.akka.AkkaEngine
import space.kscience.simba.akka.SpawnCells
import space.kscience.simba.state.Cell
import space.kscience.simba.state.ObjectState
import space.kscience.simba.utils.Vector

class AkkaStreamEngine<C: Cell<C, State>, State: ObjectState>(
    private val dimensions: Vector,
    private val neighborsIndices: Set<Vector>,
    private val init: (Vector) -> C,
    private val nextState: suspend (State, List<C>) -> State,
) : AkkaEngine() {
    override fun init() {
        actorSystem.tell(SpawnCells(dimensions, neighborsIndices) { index ->
            StreamActor(this, init(index), nextState)
        })
    }
}