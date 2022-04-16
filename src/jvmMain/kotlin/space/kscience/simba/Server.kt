package space.kscience.simba

import akka.actor.typed.ActorSystem
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import space.kscience.simba.akka.actor.AkkaActorEngine
import space.kscience.simba.akka.actor.MainActor
import space.kscience.simba.engine.Engine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

suspend fun actorsToString(field: List<ActorClassicCell>?, out: suspend (String) -> Unit) {
    field ?: return out("")

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
    out(builder.toString())
}

fun main() {
    val random = Random(0)
    val simulationEngine: Engine = AkkaActorEngine(10, 10, { _, _ -> ActorCellState(random.nextBoolean()) }, ::actorNextStep)
//    val mainActor = ActorSystem.create(MainActor.create(), "gameOfLife")
//    mainActor.tell(MainActor.Companion.SpawnCells(10, 10, { _, _ -> ActorCellState(random.nextBoolean()) }, ::actorNextStep))

    embeddedServer(Netty, 9090) {
        install(ContentNegotiation) {
            json()
        }
        install(CORS) {
            method(HttpMethod.Get)
            anyHost()
        }
        install(Compression) {
            gzip()
        }

        routing {
            get("/") {
                call.respondText(
                    this::class.java.classLoader.getResource("index.html")!!.readText(),
                    ContentType.Text.Html
                )
            }

            static("/") {
                resources("")
            }

            get("/render") {
//                suspendCoroutine<Unit> { continuation ->
//                    mainActor.tell(MainActor.Companion.Render(iteration = -1, returnIfResultIsNotReady = true) { cells ->
//                        actorsToString(cells) {
//                            call.respondText(it)
//                            continuation.resume(Unit)
//                        }
//                    })
//                }
//                mainActor.tell(MainActor.Companion.Iterate())
//                mainActor.tell(MainActor.Companion.Render(::actorsToString))
            }

            get("/status/{iteration}") {
//                suspendCoroutine<Unit> { continuation ->
//                    val iteration = call.parameters["iteration"]?.toLong() ?: error("Invalid status request")
//                    mainActor.tell(MainActor.Companion.Render(iteration, returnIfResultIsNotReady = true) { cells ->
//                        call.respond(cells ?: emptyList())
//                        continuation.resume(Unit)
//                    })
//                }
            }
        }
    }.start(wait = true)
}