package technology.idlab

import technology.idlab.runner.Pipeline
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    // Parse arguments.
    if (args.size != 1) {
        println("Usage: jvm-runner <config>")
        exitProcess(0)
    }

    // Parse and load the configuration.
    val configPath = args[0]
    val config = File(configPath)
    val pipeline = Pipeline(config)

    // Execute all functions.
    pipeline.executeSync()
}
