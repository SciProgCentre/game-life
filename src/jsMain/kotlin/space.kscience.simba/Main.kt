package space.kscience.simba

import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.html.dom.append
import kotlinx.html.js.button
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import space.kscience.simba.simulation.*

private val scope = MainScope()
private val games = listOf(
    GameOfLife(100, 100, 5),
    Boids(1000, 1000),
    Mitosis(100, 100, 5),
    SnakeGame(10, 10, 20),
)

private fun GameSystem.initGame() {
    this.initializeControls(document.getElementById("controls") as HTMLElement, scope)
    this.initializeCanvas(document.getElementById("gamefield") as HTMLCanvasElement)
    scope.launch { render(1L) }
}

fun main() {
    val simulations = document.getElementById("simulations") as HTMLElement
    simulations.append {
        games.forEach { game ->
            button { +game.name }.onclick = {
                game.initGame()
                simulations.hidden = true
                it
            }
        }
    }
}
