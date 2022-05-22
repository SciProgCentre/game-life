package space.kscience.simba.engine

import space.kscience.simba.Cell
import space.kscience.simba.EnvironmentState
import space.kscience.simba.ObjectState

sealed class Message
class AddNeighbour(val cellActor: Actor<Message>): Message()
class Iterate: Message()
class PassState<C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState>(val state: C, val timestamp: Long): Message()
