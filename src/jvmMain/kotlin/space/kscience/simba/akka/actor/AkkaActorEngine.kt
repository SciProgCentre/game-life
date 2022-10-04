package space.kscience.simba.akka.actor

import space.kscience.simba.akka.AkkaEngine
import space.kscience.simba.akka.SpawnCells
import space.kscience.simba.state.Cell
import space.kscience.simba.state.ObjectState
import space.kscience.simba.utils.Vector

class AkkaActorEngine<C: Cell<C, State>, State: ObjectState>(
    private val dimensions: Vector,
    private val neighborsIndices: Set<Vector>,
    private val init: (Vector) -> C,
    private val nextState: suspend (State, List<C>) -> State,
) : AkkaEngine() {
    override fun init() {
        actorSystem.tell(SpawnCells(dimensions, neighborsIndices) { parent, index ->
            CellActor(parent).let { it to it.create(init(index), nextState) }
        })
    }
}