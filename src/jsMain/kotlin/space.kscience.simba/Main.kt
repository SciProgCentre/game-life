package space.kscience.simba

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import space.kscience.simba.simulation.*
import space.kscience.simba.utils.SimulationSettings

private val scope = MainScope()
private val endpoint = window.location.origin
private val jsonClient = HttpClient {
    install(JsonFeature) { serializer = KotlinxSerializer() }
}

private fun GameSystem.initGame() {
    this.initializeControls(document.getElementById("controls") as HTMLElement, scope)
    this.initializeCanvas(document.getElementById("gamefield") as HTMLCanvasElement)
    scope.launch { render(0L) }
}

private suspend inline fun <reified T> request(url: String) : T {
    return jsonClient.get("$endpoint/$url")
}

fun main() {
    scope.launch {
        val settings = request<SimulationSettings>("settings")
        val simulation = when (settings.name) {
            GameOfLife.name -> GameOfLife(settings.dimensions[0], settings.dimensions[1], 50)
            Boids.name -> Boids(1000, 1000)
            Mitosis.name -> Mitosis(settings.dimensions[0], settings.dimensions[1], 5)
            SnakeGame.name -> SnakeGame(settings.dimensions[0], settings.dimensions[1], 20)
            else -> TODO("Not supported simulation ${settings.name}")
        }
        simulation.initGame()
    }
}
