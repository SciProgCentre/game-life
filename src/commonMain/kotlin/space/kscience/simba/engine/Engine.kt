package space.kscience.simba.engine

interface Engine  {
    val systems: List<EngineSystem>

    fun <T: Message> spawn(actor: Actor<T>, name: String): Actor<T>
    fun iterate()
    fun processWithSystems(msg: Message) {
        systems.forEach { it.process(msg) }
    }
}

