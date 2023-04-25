package space.kscience.simba

import java.io.File

fun clearActorLogs() {
    File("./build/actors.log").delete()
}