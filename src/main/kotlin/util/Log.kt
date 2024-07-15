package technology.idlab.util

import java.time.format.DateTimeFormatter
import java.util.*
import technology.idlab.exception.RunnerException

private const val TIME_PADDING = 15
private const val THREAD_PADDING = 8
private const val LEVEL_PADDING = 7
private const val LOCATION_PADDING = 35
private const val MESSAGE_PADDING = 50

/**
 * A simple logging class that prints messages to the console. The class is a singleton, and all
 * messages are outputted to the console. The class has three levels of logging: DEBUG, INFO, and
 * FATAL. DEBUG is for debugging information, INFO is for general information, and FATAL is for
 * errors that will cause the program to exit. At the moment of writing, all levels are outputted to
 * the console regardless of the debug flag.
 */
class Log private constructor(header: Boolean = true) {
  private enum class Level {
    DEBUG,
    INFO,
    SEVERE,
    FATAL,
  }

  init {
    if (header) {
      val builder = StringBuilder()
      builder.append("TIME".padEnd(TIME_PADDING, ' '))
      builder.append("THREAD".padEnd(THREAD_PADDING, ' '))
      builder.append("LEVEL".padEnd(LEVEL_PADDING, ' '))
      builder.append("LOCATION".padEnd(LOCATION_PADDING, ' '))
      builder.append("MESSAGE".padEnd(MESSAGE_PADDING, ' '))
      builder.append("\n----".padEnd(TIME_PADDING, ' '))
      builder.append("------".padEnd(THREAD_PADDING, ' '))
      builder.append("-----".padEnd(LEVEL_PADDING, ' '))
      builder.append("--------".padEnd(LOCATION_PADDING, ' '))
      builder.append("-------\n")

      synchronized(System.out) { print(builder) }
    }

    Runtime.getRuntime().addShutdownHook(Thread { info("The JVM is shutting down.") })
  }

  /**
   * Print a message to the output using a given level.
   *
   * @param message The message to print.
   * @param level The level of the message.
   */
  private fun output(message: String, level: Level, location: String? = null) {
    // Get the current time.
    val instant = Date().toInstant()
    val tz = instant.atZone(TimeZone.getDefault().toZoneId())
    val iso = DateTimeFormatter.ISO_LOCAL_TIME
    val time = tz.format(iso)

    // Get the thread ID.
    val thread = Thread.currentThread().id.toString()

    // Get the level.
    val levelCode = level.name

    // Get the location.
    val usedLocation =
        location
            ?: run {
              val call = Throwable().stackTrace[3]
              val clazz = call.className.substringAfterLast(".").substringBefore("$")
              val method = call.methodName.substringBefore("$")
              "${clazz}::${method}::${call.lineNumber}"
            }

    // Build the message.
    val builder = StringBuilder()

    // If the message is of level debug, set color to gray.
    if (level == Level.DEBUG) {
      builder.append("\u001B[90m")
    }

    // If the message is severe, set the color to red.
    if (level == Level.SEVERE) {
      builder.append("\u001B[31m")
    }

    // The actual message.
    builder.append(time.padEnd(TIME_PADDING, ' '))
    builder.append(thread.padEnd(THREAD_PADDING, ' '))
    builder.append(levelCode.padEnd(LEVEL_PADDING, ' '))
    builder.append(usedLocation.padEnd(LOCATION_PADDING, ' '))
    builder.append(message)
    builder.append("\n")

    // Reset coloring.
    if (level == Level.DEBUG || level == Level.SEVERE) {
      builder.append("\u001B[0m")
    }

    // Print to the console, thread safe.
    synchronized(System.out) { print(builder) }
  }

  /**
   * Print a general info message. Will get outputted in all modes.
   *
   * @param message The message to print.
   */
  fun info(message: String, location: String? = null) {
    output(message, Level.INFO, location = location)
  }

  /**
   * Print a fatal message, after which the program will exit with an error.
   *
   * @param message The message to print.
   */
  fun fatal(message: String, location: String? = null): Nothing {
    output(message, Level.FATAL, location = location)
    throw RunnerException()
  }

  /**
   * Print a message if and only if the debug flag is set. Note that the message will not be
   * evaluated lazily.
   *
   * @param message The message to print.
   */
  fun debug(message: String) {
    output(message, Level.DEBUG)
  }

  /**
   * Print a message if and only if the debug flag is set. Note that the message will be evaluated
   * lazily only if the debug flag is set.
   *
   * @param message A function determining the message to print.
   */
  fun debug(message: () -> String) {
    output(message(), Level.DEBUG)
  }

  /**
   * Print a severe message, which will be colored red in the console.
   *
   * @param message The message to print.
   */
  fun severe(message: String, location: String? = null) {
    output(message, Level.SEVERE, location = location)
  }

  companion object {
    /**
     * A globally available instance of the logger. Note that at its creation, the logger will
     * output the header to the console, which is why we only allow a single instance.
     */
    val shared = Log()
  }
}
