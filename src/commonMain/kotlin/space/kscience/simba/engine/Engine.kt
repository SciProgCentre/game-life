package space.kscience.simba.engine

interface Engine  {
    val systems: List<EngineSystem>

    fun iterate()
    fun processWithSystems(msg: Message) {
        systems.forEach { it.process(msg) }
    }
}

