package space.kscience.simba

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
import space.kscience.simba.engine.Engine
import space.kscience.simba.systems.PrintSystem
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

fun actorsToString(field: List<ActorClassicCell>): String {
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
    return builder.toString()
}

fun main() {
    val random = Random(0)
    val (n, m) = 10 to 10

    val simulationEngine: Engine = AkkaActorEngine(n, m, { _, _ -> ActorCellState(random.nextBoolean()) }, ::actorNextStep)
    val printSystem = PrintSystem(n * m)
    simulationEngine.addNewSystem(printSystem)

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

            get("/status/{iteration}") {
                simulationEngine.iterate()
                val iteration = call.parameters["iteration"]?.toLong() ?: error("Invalid status request")
                call.respond(printSystem.render(iteration + 1))
            }
        }
    }.start(wait = true)
}