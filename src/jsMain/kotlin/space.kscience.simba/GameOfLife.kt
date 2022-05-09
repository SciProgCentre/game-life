package space.kscience.simba

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.clear
import kotlinx.html.button
import kotlinx.html.dom.append
import kotlinx.html.id
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement

class GameOfLife(private val width: Int, private val height: Int, private val cellSize: Int): GameSystem {
    private lateinit var context: CanvasRenderingContext2D
    private val endpoint = window.location.origin
    private val jsonClient = HttpClient {
        install(JsonFeature) { serializer = KotlinxSerializer() }
    }

    private suspend fun getLifeData(iteration: Long): List<ActorClassicCell> {
        return jsonClient.get("$endpoint/status/gameOfLife/$iteration")
    }

    override fun initializeControls(panel: HTMLElement) {
        panel.clear()

        panel.append.button {
            id = "start"
            +"Start"
        }

        panel.append.button {
            id = "next"
            +"Next"
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
            val color = if (it.isAlive()) "#000000" else "#FFFFFF"
            context.fillStyle = color
            context.fillRect(it.i * doubleSize, it.j * doubleSize, doubleSize, doubleSize)
        }
    }
}