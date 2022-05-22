package space.kscience.simba.engine

import space.kscience.simba.state.Cell
import space.kscience.simba.state.EnvironmentState
import space.kscience.simba.state.ObjectState

sealed class Message
class AddNeighbour(val cellActor: Actor): Message()
class Iterate: Message()
class PassState<C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState>(val state: C, val timestamp: Long): Message()
