package space.kscience.simba

interface EnvironmentState
interface ObjectState

abstract class Cell<Self: Cell<Self, State, Env>, State: ObjectState, Env: EnvironmentState>: Comparable<Self> {
    abstract fun isReadyForIteration(expectedCount: Int): Boolean
    abstract fun iterate(convert: (State, Env) -> State): Self
    abstract fun addNeighboursState(cell: Self)
}
