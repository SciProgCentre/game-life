package space.kscience.simba.engine

import java.io.Serializable

/**
 * Represents single element of the simulation. Each `Actor` is independent and is controlled by messages.
 */
interface Actor : Serializable {
    /**
     * Process message by current agent.
     */
    fun handleWithoutResendingToEngine(msg: Message)

    /**
     * Send given message to the agent for further analysis.
     */
    fun sendToEngine(msg: Message)

    /**
     * Process message by current agent and resend it to engine for further analysis.
     */
    fun handle(msg: Message) {
        sendToEngine(msg)
        handleWithoutResendingToEngine(msg)
    }
}