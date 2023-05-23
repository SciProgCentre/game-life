package space.kscience.simba.engine

import space.kscience.simba.state.*
import space.kscience.simba.utils.Vector
import java.io.Serializable

sealed class Message: Serializable
class Init<State: ObjectState<State, Env>, Env: EnvironmentState>(val index: Vector, val state: State): Message()
class AddNeighbour(val cellActor: Actor): Message()
class Iterate: Message()
class PassState<State: ObjectState<State, Env>, Env: EnvironmentState>(val state: State, val timestamp: Long): Message() {
    override fun toString(): String {
        return "state at $timestamp time: $state"
    }
}
class UpdateSelfState<State: ObjectState<State, Env>, Env: EnvironmentState>(val newState: State, val timestamp: Long): Message()
class UpdateEnvironment<Env: EnvironmentState>(val env: Env): Message()
