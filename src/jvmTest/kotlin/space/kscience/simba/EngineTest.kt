package space.kscience.simba

import kotlinx.coroutines.runBlocking
import org.junit.Test
import space.kscience.simba.akka.actor.AkkaActorEngine
import space.kscience.simba.coroutines.CoroutinesActorEngine
import space.kscience.simba.engine.Engine
import space.kscience.simba.systems.PrintSystem
import kotlin.random.Random
import kotlin.test.assertEquals

class EngineTest {
    private val random = Random(0)
    private val n = 7
    private val m = 7

    @Test
    fun testAkkaGameOfLife() {
        val simulationEngine = AkkaActorEngine(n, m, { i, j -> ActorCellState(i in 2..4 && j in 2..4) }, ::actorNextStep)
        checkEngineCorrectness(simulationEngine)
    }

    @Test
    fun testAkkaGameOfLifeAfterIterations() {
        val simulationEngine = AkkaActorEngine(n, m, { i, j -> ActorCellState(i in 2..4 && j in 2..4) }, ::actorNextStep)
        checkEngineCorrectnessAfterIterations(simulationEngine)
    }

    @Test
    fun testKotlinActorsGameOfLife() {
        val simulationEngine = CoroutinesActorEngine(n, m, { i, j -> ActorCellState(i in 2..4 && j in 2..4) }, ::actorNextStep)
        checkEngineCorrectness(simulationEngine)
    }

    @Test
    fun testKotlinActorsGameOfLifeAfterIterations() {
        val simulationEngine = CoroutinesActorEngine(n, m, { i, j -> ActorCellState(i in 2..4 && j in 2..4) }, ::actorNextStep)
        checkEngineCorrectnessAfterIterations(simulationEngine)
    }

    private fun checkEngineCorrectness(simulationEngine: Engine) {
        val printSystem = PrintSystem(n * m)
        simulationEngine.addNewSystem(printSystem)

        runBlocking {
            simulationEngine.iterate()
            val field1 = actorsToString(printSystem.render(1).toList())
            assertEquals("OOOOOOO\nOOOOOOO\nOOXXXOO\nOOXXXOO\nOOXXXOO\nOOOOOOO\nOOOOOOO", field1.trim())

            simulationEngine.iterate()
            val field2 = actorsToString(printSystem.render(2).toList())
            assertEquals("OOOOOOO\nOOOXOOO\nOOXOXOO\nOXOOOXO\nOOXOXOO\nOOOXOOO\nOOOOOOO", field2.trim())

            simulationEngine.iterate()
            val field3 = actorsToString(printSystem.render(3).toList())
            assertEquals("OOOOOOO\nOOOXOOO\nOOXXXOO\nOXXOXXO\nOOXXXOO\nOOOXOOO\nOOOOOOO", field3.trim())

            simulationEngine.iterate()
            val field4 = actorsToString(printSystem.render(4).toList())
            assertEquals("OOOOOOO\nOOXXXOO\nOXOOOXO\nOXOOOXO\nOXOOOXO\nOOXXXOO\nOOOOOOO", field4.trim())
        }
    }

    private fun checkEngineCorrectnessAfterIterations(simulationEngine: Engine) {
        val printSystem = PrintSystem(n * m)
        simulationEngine.addNewSystem(printSystem)

        simulationEngine.iterate()
        simulationEngine.iterate()
        simulationEngine.iterate()
        simulationEngine.iterate()

        runBlocking {
            val field1 = actorsToString(printSystem.render(1).toList())
            assertEquals("OOOOOOO\nOOOOOOO\nOOXXXOO\nOOXXXOO\nOOXXXOO\nOOOOOOO\nOOOOOOO", field1.trim())

            val field2 = actorsToString(printSystem.render(2).toList())
            assertEquals("OOOOOOO\nOOOXOOO\nOOXOXOO\nOXOOOXO\nOOXOXOO\nOOOXOOO\nOOOOOOO", field2.trim())

            val field3 = actorsToString(printSystem.render(3).toList())
            assertEquals("OOOOOOO\nOOOXOOO\nOOXXXOO\nOXXOXXO\nOOXXXOO\nOOOXOOO\nOOOOOOO", field3.trim())

            val field4 = actorsToString(printSystem.render(4).toList())
            assertEquals("OOOOOOO\nOOXXXOO\nOXOOOXO\nOXOOOXO\nOXOOOXO\nOOXXXOO\nOOOOOOO", field4.trim())
        }
    }
}
