package technology.idlab.util

import java.time.format.DateTimeFormatter
import java.util.*

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
class Log private constructor(val header: Boolean = true) {
  private enum class Level {
    DEBUG,
    INFO,
    FATAL,
  }

  init {
    if (header) {
      print("TIME".padEnd(TIME_PADDING, ' '))
      print("THREAD".padEnd(THREAD_PADDING, ' '))
      print("LEVEL".padEnd(LEVEL_PADDING, ' '))
      print("LOCATION".padEnd(LOCATION_PADDING, ' '))
      println("MESSAGE".padEnd(MESSAGE_PADDING, ' '))
      print("----".padEnd(TIME_PADDING, ' '))
      print("------".padEnd(THREAD_PADDING, ' '))
      print("-----".padEnd(LEVEL_PADDING, ' '))
      print("--------".padEnd(LOCATION_PADDING, ' '))
      println("-------")
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
              "${clazz}::${call.methodName}::${call.lineNumber}"
            }

    // Print to the console.
    print(time.padEnd(TIME_PADDING, ' '))
    print(thread.padEnd(THREAD_PADDING, ' '))
    print(levelCode.padEnd(LEVEL_PADDING, ' '))
    print(usedLocation.padEnd(LOCATION_PADDING, ' '))
    println(message)
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
    Runtime.getRuntime().exit(1)
    throw Exception("This exception cannot be reached, but the Kotlin compiler doesn't know that.")
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
    debug(message())
  }

  companion object {
    /**
     * A globally available instance of the logger. Note that at its creation, the logger will
     * output the header to the console, which is why we only allow a single instance.
     */
    val shared = Log()
  }
}
