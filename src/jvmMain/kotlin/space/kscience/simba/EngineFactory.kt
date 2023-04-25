package space.kscience.simba

import space.kscience.simba.akka.actor.AkkaActorEngine
import space.kscience.simba.engine.Engine
import space.kscience.simba.state.Cell
import space.kscience.simba.state.ObjectState
import space.kscience.simba.utils.Vector

object EngineFactory {
    // TODO wrap all parameters into Config and create builder
    fun <C: Cell<C, State>, State: ObjectState> createEngine(
        dimensions: Vector,
        neighborsIndices: Set<Vector>,
        init: (Vector) -> C,
//        nextState: suspend (State, List<State>) -> State,
    ): Engine {
        return AkkaActorEngine(dimensions, neighborsIndices, init = init, /*nextState*/)
    }
}