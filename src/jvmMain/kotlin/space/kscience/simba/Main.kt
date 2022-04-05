package space.kscience.simba

import akka.actor.typed.ActorSystem
import kotlin.random.Random
import kotlin.system.exitProcess

//fun main() {
//    val random = Random(0)
//    val game = GameOfLife(10, 10) { _, _ -> CellState(random.nextBoolean()) }
//    println(game.toString())
//
//    for (i in 0 until 10) {
//        game.iterate()
//        println(game.toString())
//    }
//}

fun actorsToString(field: List<ActorClassicCell>) {
    val builder = StringBuilder()
    val n = field.maxOf { it.i } + 1
    val m = field.maxOf { it.j } + 1

    for (i in 0 until n) {
        for (j in 0 until m) {
            builder.append(if (field[i * n + j].isAlive()) "X" else "O")
        }
        builder.append("\n")
    }
    builder.append("\n")
    println(builder.toString())
}

fun main() {
    val random = Random(0)
    val mainActor = ActorSystem.create(MainActor.create(10, 10), "gameOfLife")
    mainActor.tell(MainActor.Companion.SpawnCells({ _, _ -> ActorCellState(random.nextBoolean()) }, ::actorNextStep))
    mainActor.tell(MainActor.Companion.Render(::actorsToString))
    mainActor.tell(MainActor.Companion.Iterate())
    mainActor.tell(MainActor.Companion.Render(::actorsToString))
}