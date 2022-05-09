package space.kscience.simba

import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement

interface GameSystem {
    fun initializeControls(panel: HTMLElement)
    fun initializeCanvas(canvas: HTMLCanvasElement)
    suspend fun render(iteration: Long)
}