package technology.idlab

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    // Parse arguments.
    if (args.size < 2) {
        println("Usage: jvm-runner <config>")
        exitProcess(-1)
    }

    // Parse and load the configuration.
    val path = args[1]
    println("Loading configuration from $path")
    val config = Configuration(path)

    // Execute all functions.
    config.executeSync()
}
