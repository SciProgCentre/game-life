package space.kscience.simba

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement

private val scope = MainScope()

private fun getGame(): GameSystem {
//    return GameOfLife(10, 10, 5)
    return Boids(1000, 1000)
}

fun main() {
    val game = getGame()
    game.initializeControls(document.getElementById("controls") as HTMLElement, scope)
    game.initializeCanvas(document.getElementById("gamefield") as HTMLCanvasElement)
    window.onload = { scope.launch { game.render(1L) } }
}
