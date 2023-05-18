package space.kscience.simba.engine

interface EngineAggregator {
    fun process(msg: Message)
}
