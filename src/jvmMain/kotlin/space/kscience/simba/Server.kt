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
import space.kscience.simba.simulation.*

private val simulations = listOf(
    GameOfLifeSimulation(), BoidsSimulation(), MitosisSimulation(), SnakeLearningSimulation(), SnakeLearningWithManagerSimulation()
)

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

            simulations.forEach { with(it) { this@routing.setUp() } }
        }
    }.start(wait = true)
}
