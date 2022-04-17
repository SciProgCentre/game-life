package space.kscience.simba

import space.kscience.simba.engine.Actor
import space.kscience.simba.engine.Message

sealed class GameOfLifeMessage: Message
class AddNeighbour(val cellActor: Actor<GameOfLifeMessage>): GameOfLifeMessage()
class Iterate: GameOfLifeMessage()
class PassState(val state: ActorClassicCell, val timestamp: Long): GameOfLifeMessage()
