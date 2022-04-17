package space.kscience.simba.systems

import space.kscience.simba.PassState
import space.kscience.simba.engine.EngineSystem
import space.kscience.simba.engine.Message

class PrintSystem: EngineSystem {
    override fun process(msg: Message) {
        if (msg is PassState) {
            println(msg.state)
        }
    }
}