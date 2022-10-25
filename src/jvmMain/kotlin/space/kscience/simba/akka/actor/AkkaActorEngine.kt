package space.kscience.simba.akka.actor

import akka.actor.typed.ActorSystem
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.javadsl.AkkaManagement
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
        configCluster()
        actorSystem.tell(SpawnCells(dimensions, neighborsIndices) { parent, index ->
            CellActor(parent).let { it to it.create(init(index), nextState) }
        })
    }

    private fun configCluster() {
        AkkaManagement.get(actorSystem).start()
        ClusterBootstrap.get(actorSystem).start()
    }

    override fun onIterate() {
        actorSystem.tell(SyncIterate)
    }
}