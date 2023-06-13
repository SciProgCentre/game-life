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
import kotlinx.serialization.SerialName
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import space.kscience.plotly.Plot
import space.kscience.plotly.layout
import space.kscience.plotly.models.ScatterMode
import space.kscience.plotly.models.Trace
import space.kscience.plotly.models.TraceType
import space.kscience.plotly.plot
import space.kscience.plotly.scatter
import space.kscience.simba.utils.Vector2

@kotlinx.serialization.Serializable
data class SnakeState(val body: List<Vector2>, val bait: Vector2?)

@kotlinx.serialization.Serializable
data class SnakeGameState(@SerialName("first") val eatenBate: Int, @SerialName("second") val history: List<SnakeState>)

class SnakeGame(private val width: Int, private val height: Int, private val cellSize: Int) : GameSystem() {
    companion object {
        const val name: String = "snake"
    }

    override val kotlinxSerializer = KotlinxSerializer(kotlinx.serialization.json.Json { allowStructuredMapKeys = true })

    private lateinit var context: CanvasRenderingContext2D
    private val learningTrace: Trace = Trace()

    private val eatenBaitByIteration = mutableListOf<Int>()
    private var iteration = 0L

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

    private suspend fun getSnakeGameState(iteration: Long): SnakeGameState {
        return jsonClient.get("$endpoint/status/$name/play/$iteration")
    }

    // TODO simplify game; just one iteration (generate food once)
    override suspend fun render(iteration: Long) {
        val (eatenBate, history) = getSnakeGameState(iteration)
        eatenBaitByIteration.add(eatenBate)
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