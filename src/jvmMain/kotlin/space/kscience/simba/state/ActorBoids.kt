package space.kscience.simba.state

import space.kscience.simba.utils.*
import kotlin.random.Random

data class BoidsEnvironment(
    val minSpeed: Double = 100.0,
    val maxSpeed: Double = 300.0,

    val perceptionRadius: Double = 200.0,
    val avoidanceRadius: Double = 100.0,
    val maxSteerForce: Double = 300.0, // how fast boid can turn

    val avoidanceWeight: Double = 1.0,
    val alignWeight: Double = 1.0,
    val cohesionWeight: Double = 1.0,

    val bound: Double = 1000.0,
    val applyAllRules: Boolean = false
) : EnvironmentState

@kotlinx.serialization.Serializable
data class ActorBoidsState(val position: Vector2, val direction: Vector2, val velocity: Vector2) : ObjectState<ActorBoidsState, BoidsEnvironment> {
    // original document http://www.cs.toronto.edu/~dt/siggraph97-course/cwr87/
    // C# implementation https://github.com/SebLague/Boids
    override suspend fun iterate(neighbours: List<ActorBoidsState>, env: BoidsEnvironment?): ActorBoidsState {
        if (env == null) error("Environment for `ActorBoidsState` wasn't set")

        val deltaTime = 1.0 / 60
        val visibleNeighbours =
            neighbours.filter { (it.position - this.position).length() <= env.perceptionRadius }
        val avoidNeighbours =
            neighbours.filter { (it.position - this.position).length() <= env.avoidanceRadius }

        fun applyFirstRule(boid: ActorBoidsState): Vector2 {
            val avgAvoidanceHeading = avoidNeighbours
                .map { it.position }
                .fold(zero) { acc, otherPosition ->
                    val distance = otherPosition - boid.position
                    acc - distance / distance.sqrLength()
                }
            // separationForce
            return env.steer(boid.velocity, avgAvoidanceHeading) * env.avoidanceWeight
        }

        fun applySecondRule(boid: ActorBoidsState): Vector2 {
            val avgFlockHeading = visibleNeighbours.fold(zero) { acc, other -> acc + other.direction }
            // alignmentForce
            return env.steer(boid.velocity, avgFlockHeading) * env.alignWeight
        }

        fun applyThirdRule(boid: ActorBoidsState): Vector2 {
            val avgFlockPosition = visibleNeighbours.fold(zero) { acc, other -> acc + other.position }
            val centreOfFlockmates = avgFlockPosition / visibleNeighbours.size.toDouble()
            val offsetToFlockmatesCentre = (centreOfFlockmates - boid.position)
            // cohesionForce
            return env.steer(boid.velocity, offsetToFlockmatesCentre) * env.cohesionWeight
        }

        var acceleration = zero
        if (visibleNeighbours.isNotEmpty() && env.applyAllRules) {
            acceleration += applyFirstRule(this)
            acceleration += applySecondRule(this)
            acceleration += applyThirdRule(this)
        }

        var newVelocity = this.velocity + acceleration * deltaTime
        val newDirection = newVelocity.normalized()
        val speed = newVelocity.length().clamp(env.minSpeed, env.maxSpeed)
        newVelocity = newDirection * speed

        val newPosition = this.position + newVelocity * deltaTime
        return ActorBoidsState(newPosition.clampAndSwap(0.0, env.bound), newDirection, newVelocity)
    }

    private fun Vector2.clampAndSwap(min: Double, max: Double): Vector2 = Vector2(first.clampAndSwap(min, max), second.clampAndSwap(min, max))

    private fun BoidsEnvironment.steer(from: Vector2, towards: Vector2): Vector2 {
        val v = towards.normalized() * this.maxSpeed - from
        return v.clampMagnitude(this.maxSteerForce)
    }

    override fun isReadyForIteration(neighbours: List<ActorBoidsState>, env: BoidsEnvironment?, expectedCount: Int): Boolean {
        return neighbours.size == expectedCount
    }

    companion object {
        public fun Random.randomBoidsState(env: BoidsEnvironment): ActorBoidsState {
            val position = this.randomVector() * env.bound
            val direction = this.randomVector()
            val velocity = direction * (env.minSpeed + env.maxSpeed) / 2.0
            return ActorBoidsState(position, direction, velocity)
        }
    }
}
