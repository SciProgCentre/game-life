package space.kscience.simba.engine

import java.io.Serializable

interface Actor: Serializable {
    fun handleWithoutResendingToEngine(msg: Message)
    fun sendToEngine(msg: Message)
    fun handle(msg: Message) {
        sendToEngine(msg)
        handleWithoutResendingToEngine(msg)
    }
}