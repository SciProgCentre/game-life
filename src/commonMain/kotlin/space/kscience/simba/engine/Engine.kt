package space.kscience.simba.engine

interface Engine {
    var started: Boolean
    val systems: MutableList<EngineSystem>

    fun init()
    fun iterate()

    fun addNewSystem(system: EngineSystem) {
        if (started) throw AssertionError("Cannot add new system because engine already started to work")
        systems += system
    }

    fun processWithSystems(msg: Message) {
        if (!started) throw AssertionError("Cannot process new message because engine wasn't started")
        systems.forEach { it.process(msg) }
    }
}

