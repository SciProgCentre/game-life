package space.kscience.simba

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement

private val scope = MainScope()

private fun getGame(): GameSystem {
//    return GameOfLife(10, 10, 5)
    return Boids(100, 100)
}

fun main() {
    val game = getGame()
    game.initializeControls(document.getElementById("controls") as HTMLElement)
    game.initializeCanvas(document.getElementById("gamefield") as HTMLCanvasElement)

    var iteration = 1L
    fun animate(game: GameSystem) {
        scope.launch {
            game.render(++iteration)
            window.requestAnimationFrame { animate(game) }
        }
    }

    window.onload = { scope.launch { game.render(iteration) } }

    val startButton = document.getElementById("start") as HTMLButtonElement
    startButton.onclick = { animate(game) }

    val nextButton = document.getElementById("next") as HTMLButtonElement
    nextButton.onclick = { scope.launch { game.render(++iteration) } }
}
