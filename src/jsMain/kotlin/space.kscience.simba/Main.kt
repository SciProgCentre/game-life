import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import space.kscience.simba.CellState
import space.kscience.simba.GameOfLife
import kotlin.random.Random

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

fun render(game: GameOfLife, context: CanvasRenderingContext2D) {
    val doubleSize = cellSize.toDouble()
    game.observe {
        val color = if (it.isAlive()) "#000000" else "#FFFFFF"
        context.fillStyle = color
        context.fillRect(it.i * doubleSize, it.j * doubleSize, doubleSize, doubleSize)
    }
}

fun main() {
    val context = initializeCanvas(width * cellSize, height * cellSize)
    val random = Random(0)
    val game = GameOfLife(width, height) { _, _ -> CellState(random.nextBoolean()) }

    window.onload = {
        render(game, context)
    }

    val startButton = document.getElementById("start") as HTMLButtonElement
    startButton.onclick = {
        window.setInterval({
            render(game, context)
            game.iterate()
        }, 300)
    }
//    val nextButton = document.getElementById("next") as HTMLButtonElement
//    nextButton.onclick = {
//        game.iterate()
//        render(game, context)
//    }
}