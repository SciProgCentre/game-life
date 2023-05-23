package space.kscience.simba.akka

import akka.actor.typed.ActorSystem
import space.kscience.simba.engine.Engine
import space.kscience.simba.engine.EngineAggregator
import space.kscience.simba.state.EnvironmentState

abstract class AkkaEngine<T, Env: EnvironmentState> : Engine<Env> {
    protected abstract val actorSystem: ActorSystem<T>

    override var started: Boolean = false
    override val aggregators: MutableList<EngineAggregator> = mutableListOf()

    private var iterateCountBeforeStart = 0

    protected abstract fun onIterate()

    override fun iterate() {
        if (!started) {
            iterateCountBeforeStart++
            return
        }
        onIterate()
    }

    fun start(beforeIterate: () -> Unit) {
        started = true
        beforeIterate()
        while (iterateCountBeforeStart-- > 0) {
            iterate()
        }
    }
}