package space.kscience.simba.simulation

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.dom.clear
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.dom.append
import kotlinx.html.id
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import space.kscience.plotly.*
import space.kscience.plotly.models.ScatterMode
import space.kscience.plotly.models.Trace
import space.kscience.plotly.models.TraceType
import space.kscience.simba.machine_learning.reinforcment_learning.game.Snake
import space.kscience.simba.state.*
import space.kscience.simba.utils.Vector2
import kotlin.random.Random

class SnakeGame(private val width: Int, private val height: Int, private val cellSize: Int) : GameSystem {
    override val name: String = "Snake"

    private lateinit var context: CanvasRenderingContext2D
    private val learningTrace: Trace = Trace()
    private val endpoint = window.location.origin
    private val jsonClient = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json { allowStructuredMapKeys = true })
        }
    }

    private val seed = 0
    private val random = Random(seed)
    private val snake = Snake(width, height, seed)
    private val eatenBaitByIteration = mutableListOf<Int>()
    private var iteration = 1L

    override fun initializeControls(panel: HTMLElement, scope: CoroutineScope) {
        panel.clear()
        panel.append.apply {
            button {
                id = "start"
                +"Start"
            }.onclick = {
                scope.launch { render(++iteration) }
            }
        }
    }

    override fun initializeCanvas(canvas: HTMLCanvasElement) {
        context = canvas.getContext("2d") as CanvasRenderingContext2D
        context.canvas.width = width * cellSize
        context.canvas.height = height * cellSize
        context.strokeRect(0.0, 0.0, context.canvas.width.toDouble(), context.canvas.height.toDouble())
        document.body?.appendChild(canvas)

        document.body?.append {
            val learningCurve = Plot()
            learningCurve.scatter {
                mode = ScatterMode.lines
                type = TraceType.scatter
            }
            learningCurve.layout {
                title = "Snake learning curve"
                xaxis.title = "Number of iteration"
                yaxis.title = "The amount of eaten food"
            }
            learningCurve.addTrace(learningTrace)
            this.div {  }.plot(learningCurve)
        }
    }

    private suspend fun getSnakeCell(iteration: Long): List<ActorSnakeCell> {
        return jsonClient.get("$endpoint/status/${name.lowercase()}/$iteration")
    }

    // TODO simplify game; just one iteration (generate food once)
    override suspend fun render(iteration: Long) {
        val history = mutableListOf<SnakeState>()
        snake.restart()
        val cell = getSnakeCell(iteration).first()

        fun nextDirection(qTable: QTable<SnakeState, SnakeAction>, currentState: SnakeState, oldDirection: Snake.Direction?): Snake.Direction {
            return qTable.getNextDirection(currentState, random.getRandomSnakeDirection(oldDirection), oldDirection, true)
        }

        eatenBaitByIteration.add(0)
        snake.play(cell.state, 100, ::nextDirection) { game, oldState, _ ->
            history += oldState
            if (game.ateBait()) eatenBaitByIteration[eatenBaitByIteration.lastIndex]++
        }

        history.forEach { (bodyWithHead, baitPosition) ->
            drawCurrentGameState(bodyWithHead, baitPosition)
            delay(100)
        }

        learningTrace.x.numbers = eatenBaitByIteration.indices
        learningTrace.y.numbers = eatenBaitByIteration

        render(iteration + 1)
    }

    private fun drawCurrentGameState(bodyWithHead: List<Vector2>, baitPosition: Vector2?) {
        val doubleSize = cellSize.toDouble()
        context.clearRect(0.0, 0.0, width * doubleSize, height * doubleSize)
        bodyWithHead.forEach {
            context.fillStyle = "#000000"
            context.fillRect(
                it.first * doubleSize, context.canvas.height - cellSize - it.second * doubleSize, doubleSize, doubleSize
            )
        }

        baitPosition?.let {
            context.fillStyle = "#00FF00"
            context.fillRect(
                it.first * doubleSize, context.canvas.height - cellSize - it.second * doubleSize, doubleSize, doubleSize
            )
        }

        context.strokeRect(0.0, 0.0, context.canvas.width.toDouble(), context.canvas.height.toDouble())
    }
}