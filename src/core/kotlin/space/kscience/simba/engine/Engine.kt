package space.kscience.simba.engine

import space.kscience.simba.state.EnvironmentState
import space.kscience.simba.utils.Vector

/**
 * This class represents a basic controller for discrete simulations of a finite number of elements. Each such element
 * must implement `space.kscience.simba.engine.Actor` interface. Each `Actor` is independent and can communicate with
 * "neighbours" through messages. Each message is represented as `space.kscience.simba.engine.Message`.
 *
 * @property started true if engine is running
 * @property dimensions describe "field" characteristics where simulation take place.
 * @property aggregators the list of handlers that process each message that was sent by agents.
 */
interface Engine<Env : EnvironmentState> {
    var started: Boolean
    val dimensions: Vector
    val aggregators: MutableList<EngineAggregator>

    /**
     * This method must be called when engine is configured and ready to work.
     */
    fun init()

    /**
     * When called, engine will begin new iteration cycle.
     */
    fun iterate()

    /**
     * Update environment value for each agent.
     */
    fun setNewEnvironment(env: Env)

    fun addNewAggregator(aggregator: EngineAggregator) {
        if (started) throw AssertionError("Cannot add new aggregator because engine already started to work")
        aggregators += aggregator
    }

    /**
     * This is a callback method that is used by actors. They call it each time when message must be processed
     * by given set of aggregators.
     */
    fun processWithAggregators(msg: Message) {
        if (!started) throw AssertionError("Cannot process new message because engine wasn't started")
        aggregators.forEach { it.process(msg) }
    }
}

