package technology.idlab

import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    // Parse arguments.
    if (args.size != 1) {
        println("Usage: jvm-runner <config>")
        exitProcess(-1)
    }

    // Parse and load the configuration.
    val relativePath = args[0]
    val absolutePath = File(relativePath).absoluteFile
    val config = Configuration(absolutePath.toString())

    // Execute all functions.
    config.executeSync()
}
