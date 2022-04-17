package space.kscience.simba

import kotlinx.coroutines.runBlocking
import space.kscience.simba.akka.actor.AkkaActorEngine
import space.kscience.simba.engine.Engine
import space.kscience.simba.systems.PrintSystem
import kotlin.random.Random
import kotlin.system.exitProcess

fun main() {
    val random = Random(0)
    val n = 5
    val m = 5

    val simulationEngine: Engine = AkkaActorEngine(n, m, { _, _ -> ActorCellState(random.nextBoolean()) }, ::actorNextStep)
//    val simulationEngine: Engine = CoroutinesActorEngine(10, 10, { _, _ -> ActorCellState(random.nextBoolean()) }, ::actorNextStep)

    val printSystem = PrintSystem(n * m)
    simulationEngine.addNewSystem(printSystem)

    runBlocking {
        simulationEngine.iterate()
        println(actorsToString(printSystem.render(1).toList()))

        simulationEngine.iterate()
        println(actorsToString(printSystem.render(2).toList()))

        exitProcess(0)
    }
}
