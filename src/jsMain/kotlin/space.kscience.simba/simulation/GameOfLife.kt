package space.kscience.simba.simulation

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.dom.clear
import kotlinx.html.button
import kotlinx.html.dom.append
import kotlinx.html.id
import kotlinx.serialization.Serializable
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement

@Serializable
class ActorGameOfLifeState(val i: Int, val j: Int, val isAlive: Boolean)

class GameOfLife(private val width: Int, private val height: Int, private val cellSize: Int): GameSystem {
    override val name: String = "Game Of Life"

    private lateinit var context: CanvasRenderingContext2D
    private val endpoint = window.location.origin
    private val jsonClient = HttpClient {
        install(JsonFeature) { serializer = KotlinxSerializer() }
    }

    private var iteration = 1L

    private suspend fun getLifeData(iteration: Long): List<ActorGameOfLifeState> {
        return jsonClient.get("$endpoint/status/gameOfLife/$iteration")
    }

    override fun initializeControls(panel: HTMLElement, scope: CoroutineScope) {
        panel.clear()
        panel.append.apply {
            button {
                id = "start"
                +"Start"
            }.onclick = {
                (document.getElementById("next") as HTMLButtonElement).disabled = true
                window.setInterval({ scope.launch { render(iteration++) } }, 500)
            }

            button {
                id = "next"
                +"Next"
            }.onclick = { scope.launch { render(iteration++) } }
        }
    }

    override fun initializeCanvas(canvas: HTMLCanvasElement) {
        context = canvas.getContext("2d") as CanvasRenderingContext2D
        context.canvas.width  = width * cellSize
        context.canvas.height = height * cellSize
        document.body!!.appendChild(canvas)
    }

    override suspend fun render(iteration: Long) {
        val field = getLifeData(iteration)
        val doubleSize = cellSize.toDouble()
        field.forEach {
            val color = if (it.isAlive) "#000000" else "#FFFFFF"
            context.fillStyle = color
            context.fillRect(it.i * doubleSize, it.j * doubleSize, doubleSize, doubleSize)
        }
    }
}