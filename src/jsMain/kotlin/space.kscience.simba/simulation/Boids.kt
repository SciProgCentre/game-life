package space.kscience.simba.simulation

import io.ktor.client.request.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.dom.clear
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.serialization.Serializable
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import space.kscience.simba.utils.*

class TriangleSprite(private val position: Vector2, private val direction: Vector2) {
    private val height = 15.0
    private val baseLength = 10.0

    fun draw(context: CanvasRenderingContext2D) {
        // construct isosceles triangle ABC with base BC
        val A = position + direction.normalized() * (2.0 / 3.0) * height
        val M = position - direction.normalized() * (1.0 / 3.0) * height // middle point of base

        val n = (A.second - M.second to -A.first + M.first).normalized() // normal to height
        val B = M + 0.5 * baseLength * n
        val C = M - 0.5 * baseLength * n

        context.fillStyle = "black"
        context.beginPath()
        context.moveTo(A.first, A.second)
        context.lineTo(B.first, B.second)
        context.lineTo(C.first, C.second)
        context.fill()
    }
}

@Serializable
class ActorBoidsState(val position: Vector2, val direction: Vector2, val velocity: Vector2)

class Boids(private val width: Int, private val height: Int) : GameSystem() {
    companion object {
        const val name: String = "boids"
    }

    private lateinit var context: CanvasRenderingContext2D

    private var iteration = 0L
    private var withAllRules = false

    private suspend fun getBoidsData(iteration: Long): List<ActorBoidsState> {
        return jsonClient.get("$endpoint/status/$name/$iteration")
    }

    override fun initializeControls(panel: HTMLElement, scope: CoroutineScope) {
        panel.clear()
        panel.append.apply {
            fun animate() {
                scope.launch {
                    render(++iteration)
                    window.requestAnimationFrame { animate() }
                }
            }

            button {
                id = "start"
                +"Start"
            }.onclick = {
                (document.getElementById("next") as HTMLButtonElement).disabled = true
                animate()
            }

            button {
                id = "next"
                +"Next"
            }.onclick = { scope.launch { render(++iteration) } }

            input(type = InputType.checkBox) { id = "withAllRules" }.onclick = {
                scope.launch {
                    withAllRules = !withAllRules
                    jsonClient.put("$endpoint/boids?withAllRules=$withAllRules")
                }
            }

            label {
                attributes += "for" to "withAllRules"
                +"Enable boids rules"
            }
        }
    }

    override fun initializeCanvas(canvas: HTMLCanvasElement) {
        context = canvas.getContext("2d") as CanvasRenderingContext2D
        context.canvas.width  = width
        context.canvas.height = height
        document.body!!.appendChild(canvas)
    }

    private fun clear() {
        context.clearRect(0.0, 0.0, width.toDouble(), height.toDouble())
    }

    override suspend fun render(iteration: Long) {
        // important to fetch data first and only then clear canvas
        val field = getBoidsData(iteration)
        clear()
        field.map { TriangleSprite(it.position, it.direction) }.forEach { sprite ->
            sprite.draw(context)
        }
    }
}