package space.kscience.simba.engine

import space.kscience.simba.state.*
import space.kscience.simba.utils.Vector
import java.io.Serializable

/**
 * Represents a "command" that agent must execute.
 */
sealed class Message : Serializable

/**
 * Initialize agent with given state. Must be sent once and only once before any other message.
 *
 * @property index ID of an agent
 * @property state initial state
 */
class Init<State : ObjectState<State, Env>, Env : EnvironmentState>(val index: Vector, val state: State) : Message()

/**
 * Tells agent that `cellActor` is his new neighbour to which he must send updates about changes in state.
 *
 * @property cellActor new neighbour of given agent
 */
class AddNeighbour(val cellActor: Actor) : Message()

/**
 * Begin new iteration process.
 */
class Iterate : Message()

/**
 * Snapshot of current state of an agent at given time. This message is used to pass information about state to
 * neighbour.
 *
 * @property state current state of an agent at time `timestamp`
 * @property timestamp time when given snapshot was taken. Count begins from `0`.
 */
class PassState<State : ObjectState<State, Env>, Env : EnvironmentState>(val state: State, val timestamp: Long) : Message() {
    override fun toString(): String {
        return "state at $timestamp time: $state"
    }
}

/**
 * Message with new state value that agent must store.
 */
class UpdateSelfState<State : ObjectState<State, Env>, Env : EnvironmentState>(val newState: State) : Message()

/**
 * Message with new environment value that agent must store.
 */
class UpdateEnvironment<Env : EnvironmentState>(val env: Env) : Message()
