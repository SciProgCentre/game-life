package space.kscience.simba

import space.kscience.simba.engine.Actor
import space.kscience.simba.engine.Engine
import space.kscience.simba.engine.Message

sealed class GameOfLifeMessage: Message
class AddNeighbour(val cellActor: Actor<GameOfLifeMessage>): GameOfLifeMessage()
class Iterate: GameOfLifeMessage()
class PassState<C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState>(val state: C, val timestamp: Long): GameOfLifeMessage()

sealed class MainActorMessage: Message
class SpawnDiscreteCells<C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState>(
    val n: Int, val m: Int, val engine: Engine, val init: (Int, Int) -> C, val nextStep: (State, Env) -> State
): MainActorMessage()
class SpawnContinuousCells<C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState>(
    val engine: Engine, val init: () -> C, val nextStep: (State, Env) -> State
): MainActorMessage()
class SyncIterate: MainActorMessage()
