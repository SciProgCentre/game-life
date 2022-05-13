package space.kscience.simba

import space.kscience.simba.engine.Actor
import space.kscience.simba.engine.Engine
import space.kscience.simba.engine.Message

sealed class GameOfLifeMessage: Message
class AddNeighbour(val cellActor: Actor<GameOfLifeMessage>): GameOfLifeMessage()
class Iterate: GameOfLifeMessage()
class PassState<C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState>(val state: C, val timestamp: Long): GameOfLifeMessage()

sealed class MainActorMessage: Message
class SpawnCells<C: Cell<C, State, Env>, State: ObjectState, Env: EnvironmentState>(
    val dimensions: Vector, val engine: Engine, val neighborsIndices: Set<Vector>, val init: (Vector) -> C, val nextStep: (State, Env) -> State
): MainActorMessage()
class SyncIterate: MainActorMessage()
