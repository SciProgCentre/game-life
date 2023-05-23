package space.kscience.simba.akka.actor

import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.cluster.sharding.typed.javadsl.ClusterSharding
import akka.cluster.sharding.typed.javadsl.Entity
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.javadsl.AkkaManagement
import akka.persistence.jdbc.testkit.javadsl.SchemaUtils
import akka.persistence.typed.PersistenceId
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import space.kscience.simba.akka.*
import space.kscience.simba.state.*
import space.kscience.simba.utils.Vector

class AkkaActorEngine<State: ObjectState<State, Env>, Env: EnvironmentState>(
    private val dimensions: Vector,
    private val neighborsIndices: Set<Vector>,
    config: Config = ConfigFactory.load(),
    val init: (Vector) -> State,
) : AkkaEngine<MainActorMessage, Env>() {
    override val actorSystem: ActorSystem<MainActorMessage> by lazy {
        ActorSystem.create(MainActor.create(this), "AkkaSystem", config)
    }

    override fun init() {
        configCluster()
        // NB: we will send `SpawnCells` message every time when new node will connect.
        // But this should not be a problem, because we will just ignore the `Init` message if actor already initialized.
        actorSystem.tell(SpawnCells(dimensions, neighborsIndices, init))
    }

    private fun configCluster() {
        AkkaManagement.get(actorSystem).start()
        ClusterBootstrap.get(actorSystem).start()
        val clusterSharding = ClusterSharding.get(actorSystem)
        clusterSharding.init(
            Entity.of(CellActor.ENTITY_TYPE) { entityContext ->
                Behaviors.setup {
                    EventAkkaActor<State, Env>(
                        it, entityContext.entityId, PersistenceId.of(entityContext.entityTypeKey.name(), entityContext.entityId)
                    )
                }
            }
        )

        if (!usingInMemoryDB()) {
            SchemaUtils.createIfNotExists(actorSystem)
        }
    }

    private fun usingInMemoryDB(): Boolean {
        val plugin = actorSystem.settings().config().getConfig("akka.persistence.journal").getString("plugin")
        return plugin.endsWith(".proxy")
    }

    override fun onIterate() {
        actorSystem.tell(SyncIterate)
    }

    override fun setNewEnvironment(env: Env) {
        actorSystem.tell(PassNewEnvironment(env))
    }
}