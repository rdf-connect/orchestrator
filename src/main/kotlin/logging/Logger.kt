package technology.idlab.logging

import PrettyFormatter
import java.lang.Exception
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess

/**
 * Create a new logger which uses the calling class name as the logger name.
 * Messages are logged to the console using the PrettyFormatter.
 */
fun createLogger(): Logger {
    val caller = Throwable().stackTrace[1]
    val logger = Logger.getLogger(caller.className)

    // Output to the console.
    logger.addHandler(
        object : ConsoleHandler() {
            init {
                this.formatter = PrettyFormatter()
                this.setOutputStream(System.out)
            }
        },
    )

    logger.level = Level.ALL
    return logger
}

/**
 * Log a message and exit the program with a status code of -1. This function
 * is intended to use in a try-catch block. Since it returns Nothing, it can be
 * the only expression in the catch block of an assignment.
 *
 * Example:
 * ```
 * const x = try {
 *     10 / 0
 * } catch (e: Exception) {
 *     logger.fatal("An error occurred: ${e.message}")
 * }
 * ```
 */
fun Logger.fatal(
    message: String,
    exception: Exception? = null,
): Nothing {
    // Log the message.
    val caller = Throwable().stackTrace[1]
    logp(Level.SEVERE, caller.className, caller.methodName, message)

    // Log the exception if it exists.
    if (exception != null) {
        logp(
            Level.SEVERE,
            caller.className,
            caller.methodName,
            exception.message,
        )
    }

    // Exit the program.
    exitProcess(-1)
}
