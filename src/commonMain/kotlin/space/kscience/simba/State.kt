package space.kscience.simba

interface EnvironmentState
interface ObjectState

abstract class Cell<E: EnvironmentState, T: ObjectState> {
    abstract fun iterate(convert: (T, E) -> T): Cell<E, T>
}
