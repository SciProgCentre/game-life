package space.kscience.simba.engine

interface Actor {
    val engine: Engine

    fun handle(msg: Message)
    fun handleAndCallSystems(msg: Message) {
        engine.processWithSystems(msg)
        handle(msg)
    }
}