package space.kscience.simba.simulation

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement

abstract class GameSystem {
    protected val endpoint = window.location.origin
    open val kotlinxSerializer = KotlinxSerializer()
    protected val jsonClient by lazy {
        HttpClient {
            install(JsonFeature) { serializer = kotlinxSerializer }
        }
    }

    abstract fun initializeControls(panel: HTMLElement, scope: CoroutineScope)
    abstract fun initializeCanvas(canvas: HTMLCanvasElement)
    abstract suspend fun render(iteration: Long)
}