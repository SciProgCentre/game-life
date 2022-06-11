package space.kscience.simba.akka

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import space.kscience.simba.engine.Actor
import space.kscience.simba.engine.Message

abstract class AkkaActor : Actor {
    abstract val akkaActor: Behavior<Message>
    lateinit var akkaActorRef: ActorRef<Message>

    override fun handle(msg: Message) {
        akkaActorRef.tell(msg)
    }
}