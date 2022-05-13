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
import kotlin.random.Random

private fun getEngine(n: Int, m: Int, random: Random): Engine {
    return AkkaActorEngine(intArrayOf(n, m), gameOfLifeNeighbours, { (i, j) -> classicCell(i, j, random.nextBoolean()) }, ::actorNextStep)
//    return CoroutinesActorEngine(intArrayOf(n, m), { (i, j) -> classicCell(i, j, random.nextBoolean()) }, ::actorNextStep)
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

private fun Routing.setUpBoids() {
    val random = Random(0)
    val n = 5
    val bound = 100.0

    fun Double.rollover(min: Double, max: Double): Double {
        if (this > max) return min
        if (this < min) return max
        return this
    }

    fun Vector2.rollover(min: Double, max: Double): Vector2 = Vector2(first.rollover(min, max), second.rollover(min, max))

    fun Random.randomBoidsState(): ActorBoidsState {
        return ActorBoidsState(this.randomVector() * bound, this.randomVector())
    }

    val neighbours = (1 until n).map { intArrayOf(it) }.toSet()

    val simulationEngine = AkkaActorEngine(intArrayOf(n), neighbours, { ActorBoidsCell(it[0], random.randomBoidsState()) }) { old, env ->
        // TODO
        return@AkkaActorEngine ActorBoidsState((old.position + old.velocity).rollover(0.0, bound), old.velocity)
    }

    val printSystem = PrintSystem<ActorBoidsCell, ActorBoidsState, ActorBoidsEnvironmentState>(n)
    simulationEngine.addNewSystem(printSystem)
    simulationEngine.iterate()

    get("/status/boids/{iteration}") {
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

//            setUpGameOfLife()
            setUpBoids()
        }
    }.start(wait = true)
}
