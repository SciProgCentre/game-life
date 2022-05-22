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

private object BoidsSettings {
    const val minSpeed = 100.0
    const val maxSpeed = 300.0

    const val perceptionRadius = 200.0
    const val avoidanceRadius = 100.0
    const val maxSteerForce = 300.0 // how fast boid can turn

    const val avoidanceWeight = 1.0
    const val alignWeight = 1.0
    const val cohesionWeight = 1.0
}

private fun Routing.setUpBoids() {
    val random = Random(0)
    val n = 100
    val bound = 1000.0

    fun Vector2.clampAndSwap(min: Double, max: Double): Vector2 = Vector2(first.clampAndSwap(min, max), second.clampAndSwap(min, max))

    fun Random.randomBoidsState(): ActorBoidsState {
        val position = this.randomVector() * bound
        val direction = random.randomVector()
        val velocity = direction * (BoidsSettings.minSpeed + BoidsSettings.maxSpeed) / 2.0
        return ActorBoidsState(position, direction, velocity)
    }

    fun steer(from: Vector2, towards: Vector2): Vector2 {
        val v = towards.normalized() * BoidsSettings.maxSpeed - from
        return v.clampMagnitude(BoidsSettings.maxSteerForce)
    }

    val neighbours = (1 until n).map { intArrayOf(it) }.toSet()
    var withAllRules = false

    // original document http://www.cs.toronto.edu/~dt/siggraph97-course/cwr87/
    // C# implementation https://github.com/SebLague/Boids
    val simulationEngine = AkkaActorEngine(intArrayOf(n), neighbours, { ActorBoidsCell(it[0], random.randomBoidsState()) }) { old, env ->
        val deltaTime = 1.0 / 60
        val visibleNeighbours = env.field.filter { (it.state.position - old.position).length() <= BoidsSettings.perceptionRadius }
        val avoidNeighbours = env.field.filter { (it.state.position - old.position).length() <= BoidsSettings.avoidanceRadius }

        fun applyFirstRule(boid: ActorBoidsState): Vector2 {
            val avgAvoidanceHeading = avoidNeighbours
                .map { it.state.position }
                .fold(zero) { acc, otherPosition ->
                    val distance = otherPosition - boid.position
                    acc - distance / distance.sqrLength()
                }
            // separationForce
            return steer(boid.velocity, avgAvoidanceHeading) * BoidsSettings.avoidanceWeight
        }

        fun applySecondRule(boid: ActorBoidsState): Vector2 {
            val avgFlockHeading = visibleNeighbours.fold(zero) { acc, other -> acc + other.state.direction }
            // alignmentForce
            return steer(boid.velocity, avgFlockHeading) * BoidsSettings.alignWeight
        }

        fun applyThirdRule(boid: ActorBoidsState): Vector2 {
            val avgFlockPosition = visibleNeighbours.fold(zero) { acc, other -> acc + other.state.position }
            val centreOfFlockmates = avgFlockPosition / visibleNeighbours.size.toDouble()
            val offsetToFlockmatesCentre = (centreOfFlockmates - boid.position)
            // cohesionForce
            return steer(boid.velocity, offsetToFlockmatesCentre) * BoidsSettings.cohesionWeight
        }

        var acceleration = zero
        if (visibleNeighbours.isNotEmpty() && withAllRules) {
            acceleration += applyFirstRule(old)
            acceleration += applySecondRule(old)
            acceleration += applyThirdRule(old)
        }

        var newVelocity = old.velocity + acceleration * deltaTime
        val newDirection = newVelocity.normalized()
        val speed = newVelocity.length().clamp(BoidsSettings.minSpeed, BoidsSettings.maxSpeed)
        newVelocity = newDirection * speed

        val newPosition = old.position + newVelocity * deltaTime
        return@AkkaActorEngine ActorBoidsState(newPosition.clampAndSwap(0.0, bound), newDirection, newVelocity)
    }

    val printSystem = PrintSystem<ActorBoidsCell, ActorBoidsState, ActorBoidsEnvironmentState>(n)
    simulationEngine.addNewSystem(printSystem)
    simulationEngine.iterate()

    get("/status/boids/{iteration}") {
        val iteration = call.parameters["iteration"]?.toLong() ?: error("Invalid status request")
        if (!printSystem.isCompleteFor(iteration)) simulationEngine.iterate()
        call.respond(printSystem.render(iteration))
    }

    put("/boids") {
        withAllRules = call.request.queryParameters["withAllRules"] == "true"
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
            setUpBoids()
        }
    }.start(wait = true)
}
