package space.kscience.simba.akka.actor

import akka.actor.typed.ActorSystem
import space.kscience.simba.akka.*
import space.kscience.simba.state.Cell
import space.kscience.simba.state.ObjectState
import space.kscience.simba.utils.Vector

class AkkaActorEngine<C: Cell<C, State>, State: ObjectState>(
    private val dimensions: Vector,
    private val neighborsIndices: Set<Vector>,
    private val init: (Vector) -> C,
    private val nextState: suspend (State, List<C>) -> State,
) : AkkaEngine<MainActorMessage>() {
    override val actorSystem: ActorSystem<MainActorMessage> by lazy {
        ActorSystem.create(MainActor.create(this), "AkkaSystem")
    }

    override fun init() {
        actorSystem.tell(SpawnCells(dimensions, neighborsIndices) { parent, index ->
            CellActor(parent).let { it to it.create(init(index), nextState) }
        })
    }

    override fun onIterate() {
        actorSystem.tell(SyncIterate)
    }
}