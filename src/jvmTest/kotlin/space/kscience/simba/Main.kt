package space.kscience.simba

import akka.actor.typed.ActorSystem
import space.kscience.simba.akka.actor.AkkaActorEngine
import space.kscience.simba.coroutines.CoroutinesActorEngine
import space.kscience.simba.engine.Engine
import kotlin.random.Random

fun main() {
    val random = Random(0)
//    val simulationEngine: Engine = AkkaActorEngine(10, 10, { _, _ -> ActorCellState(random.nextBoolean()) }, ::actorNextStep)
    val simulationEngine: Engine = CoroutinesActorEngine(10, 10, { _, _ -> ActorCellState(random.nextBoolean()) }, ::actorNextStep)

    println()

//    val sb = StringBuilder()
//    runBlocking {
//        suspendCoroutine<Unit> { continuation ->
//            mainActor.tell(MainActor.Companion.Render(returnIfResultIsNotReady = false) {
//                actorsToString(it) {
//                    sb.append(it)
//                    continuation.resume(Unit)
//                }
//            })
//        }
//    }
//
//    println(sb)
//    exitProcess(0)
}