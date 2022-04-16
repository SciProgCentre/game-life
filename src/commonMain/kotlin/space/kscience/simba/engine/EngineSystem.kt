package space.kscience.simba.engine

interface EngineSystem {
    fun process(msg: Message)
}

class RenderSystem() : EngineSystem {
    override fun process(msg: Message) {
        println(msg.toString())
    }
}