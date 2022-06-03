package space.kscience.simba.engine

interface Actor {
    val engine: Engine

    fun handle(msg: Message)
    fun handleAndCallSystems(msg: Message) {
        if (!engine.started) throw AssertionError("Cannot process new message because engine wasn't started")
        engine.processWithSystems(msg)
        handle(msg)
    }
}