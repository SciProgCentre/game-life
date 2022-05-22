package space.kscience.simba.engine

interface Engine {
    val systems: MutableList<EngineSystem>

    fun addNewSystem(system: EngineSystem) {
        systems += system
    }

    fun iterate()
    fun processWithSystems(msg: Message) {
        systems.forEach { it.process(msg) }
    }
}

