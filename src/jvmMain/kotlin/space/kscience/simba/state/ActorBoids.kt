package space.kscience.simba.state

import space.kscience.simba.utils.*
import kotlin.random.Random

// TODO extract to environment
private object BoidsSettings {
    const val minSpeed = 100.0
    const val maxSpeed = 300.0

    const val perceptionRadius = 200.0
    const val avoidanceRadius = 100.0
    const val maxSteerForce = 300.0 // how fast boid can turn

    const val avoidanceWeight = 1.0
    const val alignWeight = 1.0
    const val cohesionWeight = 1.0

    const val bound = 1000.0
    const val applyAllRules = true
}

@kotlinx.serialization.Serializable
data class ActorBoidsState(val position: Vector2, val direction: Vector2, val velocity: Vector2) : ObjectState<ActorBoidsState, EnvironmentState> {
    // original document http://www.cs.toronto.edu/~dt/siggraph97-course/cwr87/
    // C# implementation https://github.com/SebLague/Boids
    override suspend fun iterate(neighbours: List<ActorBoidsState>, env: EnvironmentState?): ActorBoidsState {
        val deltaTime = 1.0 / 60
        val visibleNeighbours =
            neighbours.filter { (it.position - this.position).length() <= BoidsSettings.perceptionRadius }
        val avoidNeighbours =
            neighbours.filter { (it.position - this.position).length() <= BoidsSettings.avoidanceRadius }

        fun applyFirstRule(boid: ActorBoidsState): Vector2 {
            val avgAvoidanceHeading = avoidNeighbours
                .map { it.position }
                .fold(zero) { acc, otherPosition ->
                    val distance = otherPosition - boid.position
                    acc - distance / distance.sqrLength()
                }
            // separationForce
            return steer(boid.velocity, avgAvoidanceHeading) * BoidsSettings.avoidanceWeight
        }

        fun applySecondRule(boid: ActorBoidsState): Vector2 {
            val avgFlockHeading = visibleNeighbours.fold(zero) { acc, other -> acc + other.direction }
            // alignmentForce
            return steer(boid.velocity, avgFlockHeading) * BoidsSettings.alignWeight
        }

        fun applyThirdRule(boid: ActorBoidsState): Vector2 {
            val avgFlockPosition = visibleNeighbours.fold(zero) { acc, other -> acc + other.position }
            val centreOfFlockmates = avgFlockPosition / visibleNeighbours.size.toDouble()
            val offsetToFlockmatesCentre = (centreOfFlockmates - boid.position)
            // cohesionForce
            return steer(boid.velocity, offsetToFlockmatesCentre) * BoidsSettings.cohesionWeight
        }

        var acceleration = zero
        if (visibleNeighbours.isNotEmpty() && BoidsSettings.applyAllRules) {
            acceleration += applyFirstRule(this)
            acceleration += applySecondRule(this)
            acceleration += applyThirdRule(this)
        }

        var newVelocity = this.velocity + acceleration * deltaTime
        val newDirection = newVelocity.normalized()
        val speed = newVelocity.length().clamp(BoidsSettings.minSpeed, BoidsSettings.maxSpeed)
        newVelocity = newDirection * speed

        val newPosition = this.position + newVelocity * deltaTime
        return ActorBoidsState(newPosition.clampAndSwap(0.0, BoidsSettings.bound), newDirection, newVelocity)
    }

    private fun Vector2.clampAndSwap(min: Double, max: Double): Vector2 = Vector2(first.clampAndSwap(min, max), second.clampAndSwap(min, max))

    private fun steer(from: Vector2, towards: Vector2): Vector2 {
        val v = towards.normalized() * BoidsSettings.maxSpeed - from
        return v.clampMagnitude(BoidsSettings.maxSteerForce)
    }

    override fun isReadyForIteration(neighbours: List<ActorBoidsState>, env: EnvironmentState?, expectedCount: Int): Boolean {
        return neighbours.size == expectedCount
    }

    companion object {
        public fun Random.randomBoidsState(): ActorBoidsState {
            val position = this.randomVector() * BoidsSettings.bound
            val direction = this.randomVector()
            val velocity = direction * (BoidsSettings.minSpeed + BoidsSettings.maxSpeed) / 2.0
            return ActorBoidsState(position, direction, velocity)
        }
    }
}
