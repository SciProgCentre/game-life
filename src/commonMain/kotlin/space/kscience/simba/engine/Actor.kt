package space.kscience.simba.engine

interface Actor {
    fun handleWithoutResendingToEngine(msg: Message)
    fun sendToEngine(msg: Message)
    fun handle(msg: Message) {
        sendToEngine(msg)
        handleWithoutResendingToEngine(msg)
    }
}