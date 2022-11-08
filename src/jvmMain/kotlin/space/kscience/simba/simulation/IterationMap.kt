package space.kscience.simba.simulation

import space.kscience.simba.state.*

val iterationMap = mutableMapOf<Class<*>, suspend (ObjectState, List<ObjectState>) -> ObjectState>(
    ActorGameOfLifeCell::class.java to (::actorNextStep as suspend (ObjectState, List<ObjectState>) -> ObjectState),
    ActorBoidsCell::class.java to ((BoidsSimulation.Companion::nextStep) as suspend (ObjectState, List<ObjectState>) -> ObjectState),
    ActorMitosisCell::class.java to ((MitosisSimulation.Companion::nextStep) as suspend (ObjectState, List<ObjectState>) -> ObjectState),
    ActorSnakeCell::class.java to ((SnakeLearningSimulation.Companion::nextState) as suspend (ObjectState, List<ObjectState>) -> ObjectState),
)