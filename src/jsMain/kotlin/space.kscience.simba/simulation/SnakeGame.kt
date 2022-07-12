package space.kscience.simba.simulation

import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.dom.clear
import kotlinx.html.button
import kotlinx.html.dom.append
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import space.kscience.simba.machine_learning.reinforcment_learning.game.Snake

class SnakeGame(private val width: Int, private val height: Int, private val cellSize: Int): GameSystem {
    override val name: String = "Snake"

    private lateinit var context: CanvasRenderingContext2D
    private val snake = Snake(width, height)

    override fun initializeControls(panel: HTMLElement, scope: CoroutineScope) {
        panel.clear()
        panel.append.apply {
            button { +"UP" }.onclick = {
                snake.move(Snake.Direction.UP)
                scope.launch { render(0) }
            }

            button { +"DOWN" }.onclick = {
                snake.move(Snake.Direction.DOWN)
                scope.launch { render(0) }
            }

            button { +"LEFT" }.onclick = {
                snake.move(Snake.Direction.LEFT)
                scope.launch { render(0) }
            }

            button { +"RIGHT" }.onclick = {
                snake.move(Snake.Direction.RIGHT)
                scope.launch { render(0) }
            }
        }
    }

    override fun initializeCanvas(canvas: HTMLCanvasElement) {
        context = canvas.getContext("2d") as CanvasRenderingContext2D
        context.canvas.width  = width * cellSize
        context.canvas.height = height * cellSize
        context.strokeRect(0.0, 0.0, context.canvas.width.toDouble(), context.canvas.height.toDouble());
        document.body!!.appendChild(canvas)
    }

    override suspend fun render(iteration: Long) {
        val doubleSize = cellSize.toDouble()
        context.clearRect(0.0, 0.0, width * doubleSize, height * doubleSize)
        snake.getBodyWithHead().forEach {
            console.log(it)
            context.fillStyle = "#000000"
            context.fillRect(it.first * doubleSize,  context.canvas.height - cellSize - it.second * doubleSize, doubleSize, doubleSize)
        }

        snake.getBaitPosition()?.let {
            console.log(it)
            context.fillStyle = "#00FF00"
            context.fillRect(it.first * doubleSize, context.canvas.height - cellSize - it.second * doubleSize, doubleSize, doubleSize)
        }

        context.strokeRect(0.0, 0.0, context.canvas.width.toDouble(), context.canvas.height.toDouble());
    }
}