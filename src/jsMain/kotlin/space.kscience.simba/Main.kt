package space.kscience.simba

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement

private val scope = MainScope()

private fun getGame(): GameOfLife {
    return GameOfLife(10, 10, 5)
}

fun main() {
    val game = getGame()
    game.initializeControls(document.getElementById("controls") as HTMLElement)
    game.initializeCanvas(document.getElementById("gamefield") as HTMLCanvasElement)

    var iteration = 1L
    window.onload = {
        scope.launch { game.render(iteration) }
    }

    val startButton = document.getElementById("start") as HTMLButtonElement
    startButton.onclick = {
        window.setInterval({ scope.launch { game.render(++iteration) } }, 300)
    }

    val nextButton = document.getElementById("next") as HTMLButtonElement
    nextButton.onclick = {
        scope.launch { game.render(++iteration) }
    }
}
