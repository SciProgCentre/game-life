package space.kscience.simba

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement

private val endpoint = window.location.origin
private val scope = MainScope()
private val jsonClient = HttpClient {
    install(JsonFeature) { serializer = KotlinxSerializer() }
}

const val width = 250
const val height = 100
const val cellSize = 5

fun initializeCanvas(width: Int, height: Int): CanvasRenderingContext2D {
    val canvas = document.getElementById("gamefield") as HTMLCanvasElement
    val context = canvas.getContext("2d") as CanvasRenderingContext2D
    context.canvas.width  = width
    context.canvas.height = height
    document.body!!.appendChild(canvas)
    return context
}

fun render(field: List<ActorClassicCell>, context: CanvasRenderingContext2D) {
    val doubleSize = cellSize.toDouble()
    field.forEach {
        val color = if (it.isAlive()) "#000000" else "#FFFFFF"
        context.fillStyle = color
        context.fillRect(it.i * doubleSize, it.j * doubleSize, doubleSize, doubleSize)
    }
}

suspend fun getLifeData(iteration: Long): List<ActorClassicCell> {
    return jsonClient.get("$endpoint/status/$iteration")
}

fun main() {
    val context = initializeCanvas(width * cellSize, height * cellSize)
    var iteration = 1L

    window.onload = {
        scope.launch {
            render(getLifeData(iteration), context)
        }
    }

    val startButton = document.getElementById("start") as HTMLButtonElement
    startButton.onclick = {
        window.setInterval({
            scope.launch { render(getLifeData(++iteration), context) }
        }, 300)
    }

    val nextButton = document.getElementById("next") as HTMLButtonElement
    nextButton.onclick = {
        scope.launch {
            render(getLifeData(++iteration), context)
        }
    }
}
