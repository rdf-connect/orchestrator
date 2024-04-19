package technology.idlab.logging

import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess

fun createLogger(): Logger {
    val caller = Throwable().stackTrace[1]
    val logger = Logger.getLogger(caller.className)
    logger.addHandler(StandardOutput())
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
fun Logger.fatal(message: String): Nothing {
    val caller = Throwable().stackTrace[1]
    logp(Level.SEVERE, caller.className, caller.methodName, message)
    exitProcess(-1)
}
