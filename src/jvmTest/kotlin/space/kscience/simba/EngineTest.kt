package space.kscience.simba

import kotlinx.coroutines.runBlocking
import org.junit.Test
import space.kscience.simba.akka.actor.AkkaActorEngine
import space.kscience.simba.akka.stream.AkkaStreamEngine
import space.kscience.simba.coroutines.CoroutinesActorEngine
import space.kscience.simba.engine.Engine
import space.kscience.simba.state.*
import space.kscience.simba.systems.PrintSystem
import space.kscience.simba.utils.Vector
import kotlin.random.Random
import kotlin.test.assertEquals

class EngineTest {
    private val n = 7
    private val m = 7

    private fun fillSquare(vector: Vector): ActorGameOfLifeCell {
        val (i, j) = vector
        return classicCell(i, j, i in 2..4 && j in 2..4)
    }

    @Test
    fun testAkkaGameOfLife() {
        val simulationEngine = AkkaActorEngine(intArrayOf(n, m), gameOfLifeNeighbours, ::fillSquare, ::actorNextStep)
        checkEngineCorrectness(simulationEngine)
    }

    @Test
    fun testAkkaGameOfLifeAfterIterations() {
        val simulationEngine = AkkaActorEngine(intArrayOf(n, m), gameOfLifeNeighbours, ::fillSquare, ::actorNextStep)
        checkEngineCorrectnessAfterIterations(simulationEngine)
    }

    @Test
    fun testKotlinActorsGameOfLife() {
        val simulationEngine = CoroutinesActorEngine(intArrayOf(n, m), gameOfLifeNeighbours, ::fillSquare, ::actorNextStep)
        checkEngineCorrectness(simulationEngine)
    }

    @Test
    fun testKotlinActorsGameOfLifeAfterIterations() {
        val simulationEngine = CoroutinesActorEngine(intArrayOf(n, m), gameOfLifeNeighbours, ::fillSquare, ::actorNextStep)
        checkEngineCorrectnessAfterIterations(simulationEngine)
    }

    @Test
    fun testAkkaStreamGameOfLife() {
        val simulationEngine = AkkaStreamEngine(intArrayOf(n, m), gameOfLifeNeighbours, ::fillSquare, ::actorNextStep)
        checkEngineCorrectness(simulationEngine)
    }

    @Test
    fun testAkkaStreamGameOfLifeAfterIterations() {
        val simulationEngine = AkkaStreamEngine(intArrayOf(n, m), gameOfLifeNeighbours, ::fillSquare, ::actorNextStep)
        checkEngineCorrectnessAfterIterations(simulationEngine)
    }

    @Test
    fun testAkkaActorVsCoroutines() {
        val random1 = Random(0)
        val random2 = Random(0)

        val bigN = 100
        val bigM = 100

        val akkaEngine = AkkaActorEngine(
            intArrayOf(bigN, bigM),
            gameOfLifeNeighbours,
            { (i, j) -> classicCell(i, j, random1.nextBoolean()) },
            ::actorNextStep
        )
        val coroutinesEngine = CoroutinesActorEngine(
            intArrayOf(bigN, bigM),
            gameOfLifeNeighbours,
            { (i, j) -> classicCell(i, j, random2.nextBoolean()) },
            ::actorNextStep
        )

        checkEnginesEquality(akkaEngine, coroutinesEngine, bigM * bigN, 10)
    }

    @Test
    fun testAkkaActorVsAkkaStream() {
        val random1 = Random(0)
        val random2 = Random(0)

        // we are using small field, but more iterations here because of problems with memory in streams
        val bigN = 10
        val bigM = 10

        val akkaEngine = AkkaActorEngine(
            intArrayOf(bigN, bigM),
            gameOfLifeNeighbours,
            { (i, j) -> classicCell(i, j, random1.nextBoolean()) },
            ::actorNextStep
        )
        val akkaStreamEngine = AkkaStreamEngine(
            intArrayOf(bigN, bigM),
            gameOfLifeNeighbours,
            { (i, j) -> classicCell(i, j, random2.nextBoolean()) },
            ::actorNextStep
        )
        checkEnginesEquality(akkaEngine, akkaStreamEngine, bigM * bigN, 1000)
    }

    private fun checkEnginesEquality(firstEngine: Engine, secondEngine: Engine, size: Int, iterations: Int) {
        val firstPrintSystem = PrintSystem<ActorGameOfLifeCell, ActorGameOfLifeState, ActorGameOfLifeEnv>(size)
        firstEngine.addNewSystem(firstPrintSystem)
        firstEngine.init()

        val secondPrintSystem = PrintSystem<ActorGameOfLifeCell, ActorGameOfLifeState, ActorGameOfLifeEnv>(size)
        secondEngine.addNewSystem(secondPrintSystem)
        secondEngine.init()

        for (i in 0..iterations) {
            runBlocking {
                firstEngine.iterate()
                secondEngine.iterate()

                val firstField = actorsToString(firstPrintSystem.render(i + 1L).toList())
                val secondField = actorsToString(secondPrintSystem.render(i + 1L).toList())

                assertEquals(firstField.trim(), secondField.trim())
            }
        }
    }

    private fun checkEngineCorrectness(simulationEngine: Engine) {
        val printSystem = PrintSystem<ActorGameOfLifeCell, ActorGameOfLifeState, ActorGameOfLifeEnv>(n * m)
        simulationEngine.addNewSystem(printSystem)
        simulationEngine.init()

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
        val printSystem = PrintSystem<ActorGameOfLifeCell, ActorGameOfLifeState, ActorGameOfLifeEnv>(n * m)
        simulationEngine.addNewSystem(printSystem)
        simulationEngine.init()

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
