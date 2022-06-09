package space.kscience.simba.akka

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import space.kscience.simba.engine.Actor
import space.kscience.simba.engine.Message

abstract class AkkaActor(
    private val spawnAkkaActor: (Behavior<Message>) -> ActorRef<Message>
) : Actor {
    abstract val akkaActor: Behavior<Message>
    private val akkaActorRef: ActorRef<Message> by lazy { spawnAkkaActor(akkaActor) }

    override fun handle(msg: Message) {
        akkaActorRef.tell(msg)
    }
}