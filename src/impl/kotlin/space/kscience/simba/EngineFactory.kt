package space.kscience.simba

import space.kscience.simba.akka.actor.AkkaActorEngine
import space.kscience.simba.engine.Engine
import space.kscience.simba.state.*
import space.kscience.simba.utils.Vector

object EngineFactory {
    // TODO wrap all parameters into Config and create builder
    /**
     * Use this method to get some implementation of `space.kscience.simba.engine.Engine`.
     */
    fun <State : ObjectState<State, Env>, Env : EnvironmentState> createEngine(
        dimensions: Vector,
        neighborsIndices: Set<Vector>,
        init: (Vector) -> State,
    ): Engine<Env> {
        return AkkaActorEngine(dimensions, neighborsIndices, init = init)
    }
}