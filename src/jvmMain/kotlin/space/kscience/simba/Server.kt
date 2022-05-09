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
import space.kscience.simba.coroutines.CoroutinesActorEngine
import space.kscience.simba.engine.Engine
import space.kscience.simba.systems.PrintSystem
import kotlin.random.Random

private fun getEngine(n: Int, m: Int, random: Random): Engine {
    return AkkaActorEngine(n, m, { i, j -> classicCell(i, j, random.nextBoolean()) }, ::actorNextStep)
//    return CoroutinesActorEngine(n, m, { _, _ -> ActorCellState(random.nextBoolean()) }, ::actorNextStep)
}

private fun Routing.setUpGameOfLife() {
    val random = Random(0)
    val (n, m) = 10 to 10

    val simulationEngine = getEngine(n, m, random)
    val printSystem = PrintSystem<ActorClassicCell, ActorCellState, ActorCellEnvironmentState>(n * m)
    simulationEngine.addNewSystem(printSystem)
    simulationEngine.iterate()

    get("/status/gameOfLife/{iteration}") {
        simulationEngine.iterate()
        val iteration = call.parameters["iteration"]?.toLong() ?: error("Invalid status request")
        call.respond(printSystem.render(iteration))
    }
}

fun main() {
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

            setUpGameOfLife()
        }
    }.start(wait = true)
}
