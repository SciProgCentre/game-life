package space.kscience.simba.akka

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import space.kscience.simba.engine.Actor
import space.kscience.simba.engine.Message
import space.kscience.simba.state.Cell
import space.kscience.simba.state.ObjectState

abstract class AkkaActor : Actor {
    lateinit var akkaActorRef: ActorRef<Message>

    abstract fun <C: Cell<C, State>, State: ObjectState> create(): Behavior<Message>
}