package space.kscience.simba.engine

interface Actor<T: Message> {
    val engine: Engine

    fun handle(msg: T)
    fun handleAndCallSystems(msg: T) {
        engine.processWithSystems(msg)
        handle(msg)
    }
}