package space.kscience.simba.engine

import space.kscience.simba.state.EnvironmentState
import space.kscience.simba.utils.Vector

interface Engine<Env: EnvironmentState> {
    var started: Boolean
    val dimensions: Vector
    val aggregators: MutableList<EngineAggregator>

    fun init()
    fun iterate()
    fun setNewEnvironment(env: Env)

    fun addNewAggregator(aggregator: EngineAggregator) {
        if (started) throw AssertionError("Cannot add new aggregator because engine already started to work")
        aggregators += aggregator
    }

    fun processWithAggregators(msg: Message) {
        if (!started) throw AssertionError("Cannot process new message because engine wasn't started")
        aggregators.forEach { it.process(msg) }
    }
}

