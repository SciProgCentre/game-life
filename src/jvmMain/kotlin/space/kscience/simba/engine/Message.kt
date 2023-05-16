package space.kscience.simba.engine

import space.kscience.simba.state.Cell
import space.kscience.simba.state.EnvironmentState
import space.kscience.simba.state.ObjectState
import java.io.Serializable

sealed class Message: Serializable
class Init<C: Cell<C, State>, State: ObjectState>(val state: C/*, val nextState: suspend (State, List<State>) -> State*/): Message()
class AddNeighbour(val cellActor: Actor): Message()
class Iterate: Message()
class PassState<State: ObjectState>(val state: State, val timestamp: Long): Message() {
    override fun toString(): String {
        return "state at $timestamp time: $state"
    }
}
class UpdateSelfState<C: Cell<C, State>, State: ObjectState>(val newCell: C, val timestamp: Long): Message()
class UpdateEnvironment<Env: EnvironmentState>(val env: Env): Message()
