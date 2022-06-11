package space.kscience.simba.akka

import akka.actor.typed.ActorSystem
import space.kscience.simba.engine.Engine
import space.kscience.simba.engine.EngineSystem

abstract class AkkaEngine : Engine {
    protected val actorSystem: ActorSystem<MainActorMessage> by lazy {
        ActorSystem.create(MainActor.create(this), "AkkaSystem")
    }

    override var started: Boolean = false
    override val systems: MutableList<EngineSystem> = mutableListOf()

    private var iterateCountBeforeStart = 0

    override fun iterate() {
        if (!started) {
            iterateCountBeforeStart++
            return
        }
        actorSystem.tell(SyncIterate)
    }

    fun start(beforeIterate: () -> Unit) {
        started = true
        beforeIterate()
        while (iterateCountBeforeStart-- > 0) {
            iterate()
        }
    }
}