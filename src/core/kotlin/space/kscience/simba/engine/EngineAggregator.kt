package space.kscience.simba.engine

/**
 * This is an endpoint for the message. Each message that was sent by an agent will be delivered to aggregator, where
 * we can analyze them.
 */
interface EngineAggregator {
    fun process(msg: Message)
}
