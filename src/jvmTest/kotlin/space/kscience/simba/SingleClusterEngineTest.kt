package space.kscience.simba

import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import space.kscience.simba.akka.actor.AkkaActorEngine
import space.kscience.simba.akka.stream.AkkaStreamEngine
import space.kscience.simba.coroutines.CoroutinesActorEngine
import space.kscience.simba.engine.Engine
import space.kscience.simba.state.*
import space.kscience.simba.systems.PrintSystem
import space.kscience.simba.utils.Vector
import java.util.*
import kotlin.test.assertEquals

class SingleClusterEngineTest {
    private val n = 7
    private val m = 7

    private val testLocalConfig = ConfigFactory.load("local1_test.conf")

    private fun fillSquare(vector: Vector): ActorGameOfLifeState {
        val (i, j) = vector
        return classicState(i, j, i in 2..4 && j in 2..4)
    }

    @After
    fun tearDown() {
        clearActorLogs()
    }

    @Test
    fun testAkkaGameOfLife() {
        val simulationEngine = AkkaActorEngine(
            intArrayOf(n, m), gameOfLifeNeighbours, testLocalConfig, ::fillSquare
        )
        checkEngineCorrectness(simulationEngine)
    }

    @Test
    fun testAkkaGameOfLifeAfterIterations() {
        val simulationEngine = AkkaActorEngine(
            intArrayOf(n, m), gameOfLifeNeighbours, testLocalConfig, ::fillSquare
        )
        checkEngineCorrectnessAfterIterations(simulationEngine)
    }

    @Test
    fun testKotlinActorsGameOfLife() {
        val simulationEngine = CoroutinesActorEngine(
            intArrayOf(n, m), gameOfLifeNeighbours, ::fillSquare
        )
        checkEngineCorrectness(simulationEngine)
    }

    @Test
    fun testKotlinActorsGameOfLifeAfterIterations() {
        val simulationEngine = CoroutinesActorEngine(
            intArrayOf(n, m), gameOfLifeNeighbours, ::fillSquare
        )
        checkEngineCorrectnessAfterIterations(simulationEngine)
    }

    @Test
    fun testAkkaStreamGameOfLife() {
        val simulationEngine = AkkaStreamEngine(
            intArrayOf(n, m), gameOfLifeNeighbours, ::fillSquare
        )
        checkEngineCorrectness(simulationEngine)
    }

    @Test
    fun testAkkaStreamGameOfLifeAfterIterations() {
        val simulationEngine = AkkaStreamEngine(
            intArrayOf(n, m), gameOfLifeNeighbours, ::fillSquare
        )
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
            testLocalConfig,
        ) { (i, j) -> classicState(i, j, random1.nextBoolean()) }
        val coroutinesEngine = CoroutinesActorEngine(
            intArrayOf(bigN, bigM),
            gameOfLifeNeighbours
        ) { (i, j) -> classicState(i, j, random2.nextBoolean()) }

        checkEnginesEquality(akkaEngine, coroutinesEngine, bigM * bigN, 10)
    }

    @Test
    fun testAkkaActorVsAkkaStream() {
        val random1 = Random(0)
        val random2 = Random(0)

        // we are using small field here because of problems with memory in streams
        val bigN = 10
        val bigM = 10

        val akkaEngine = AkkaActorEngine(
            intArrayOf(bigN, bigM),
            gameOfLifeNeighbours,
            testLocalConfig,
        ) { (i, j) -> classicState(i, j, random1.nextBoolean()) }
        val akkaStreamEngine = AkkaStreamEngine(
            intArrayOf(bigN, bigM),
            gameOfLifeNeighbours
        ) { (i, j) -> classicState(i, j, random2.nextBoolean()) }
        checkEnginesEquality(akkaEngine, akkaStreamEngine, bigM * bigN, 1000)
    }

    private fun checkEnginesEquality(firstEngine: Engine<*>, secondEngine: Engine<*>, size: Int, iterations: Int) {
        val firstPrintSystem = PrintSystem<ActorGameOfLifeState, EnvironmentState>(size)
        firstEngine.addNewSystem(firstPrintSystem)
        firstEngine.init()

        val secondPrintSystem = PrintSystem<ActorGameOfLifeState, EnvironmentState>(size)
        secondEngine.addNewSystem(secondPrintSystem)
        secondEngine.init()

        for (i in 0L..iterations) {
            runBlocking {
                println("Iteration #$i")

                firstEngine.iterate()
                secondEngine.iterate()

                val firstField = actorsToString(firstPrintSystem.render(i).toList())
                val secondField = actorsToString(secondPrintSystem.render(i).toList())

                assertEquals(firstField.trim(), secondField.trim())
            }
        }
    }

    @Suppress("SpellCheckingInspection")
    private fun checkEngineCorrectness(simulationEngine: Engine<*>) {
        val printSystem = PrintSystem<ActorGameOfLifeState, EnvironmentState>(n * m)
        simulationEngine.addNewSystem(printSystem)
        simulationEngine.init()

        runBlocking {
            simulationEngine.iterate()
            val field1 = actorsToString(printSystem.render(0).toList())
            assertEquals("OOOOOOO\nOOOOOOO\nOOXXXOO\nOOXXXOO\nOOXXXOO\nOOOOOOO\nOOOOOOO", field1.trim())

            simulationEngine.iterate()
            val field2 = actorsToString(printSystem.render(1).toList())
            assertEquals("OOOOOOO\nOOOXOOO\nOOXOXOO\nOXOOOXO\nOOXOXOO\nOOOXOOO\nOOOOOOO", field2.trim())

            simulationEngine.iterate()
            val field3 = actorsToString(printSystem.render(2).toList())
            assertEquals("OOOOOOO\nOOOXOOO\nOOXXXOO\nOXXOXXO\nOOXXXOO\nOOOXOOO\nOOOOOOO", field3.trim())

            simulationEngine.iterate()
            val field4 = actorsToString(printSystem.render(3).toList())
            assertEquals("OOOOOOO\nOOXXXOO\nOXOOOXO\nOXOOOXO\nOXOOOXO\nOOXXXOO\nOOOOOOO", field4.trim())
        }
    }

    @Suppress("SpellCheckingInspection")
    private fun checkEngineCorrectnessAfterIterations(simulationEngine: Engine<*>) {
        val printSystem = PrintSystem<ActorGameOfLifeState, EnvironmentState>(n * m)
        simulationEngine.addNewSystem(printSystem)
        simulationEngine.init()

        simulationEngine.iterate()
        simulationEngine.iterate()
        simulationEngine.iterate()
        simulationEngine.iterate()

        runBlocking {
            val field1 = actorsToString(printSystem.render(0).toList())
            assertEquals("OOOOOOO\nOOOOOOO\nOOXXXOO\nOOXXXOO\nOOXXXOO\nOOOOOOO\nOOOOOOO", field1.trim())

            val field2 = actorsToString(printSystem.render(1).toList())
            assertEquals("OOOOOOO\nOOOXOOO\nOOXOXOO\nOXOOOXO\nOOXOXOO\nOOOXOOO\nOOOOOOO", field2.trim())

            val field3 = actorsToString(printSystem.render(2).toList())
            assertEquals("OOOOOOO\nOOOXOOO\nOOXXXOO\nOXXOXXO\nOOXXXOO\nOOOXOOO\nOOOOOOO", field3.trim())

            val field4 = actorsToString(printSystem.render(3).toList())
            assertEquals("OOOOOOO\nOOXXXOO\nOXOOOXO\nOXOOOXO\nOXOOOXO\nOOXXXOO\nOOOOOOO", field4.trim())
        }
    }
}
