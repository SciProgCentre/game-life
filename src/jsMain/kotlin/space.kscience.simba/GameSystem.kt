package space.kscience.simba

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement

interface GameSystem {
    fun initializeControls(panel: HTMLElement, scope: CoroutineScope)
    fun initializeCanvas(canvas: HTMLCanvasElement)
    suspend fun render(iteration: Long)
}