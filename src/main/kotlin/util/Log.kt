package technology.idlab.util

import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.system.exitProcess
import technology.idlab.exception.RunnerException

private const val TIME_PADDING = 15
private const val TASK_PADDING = 8
private const val LEVEL_PADDING = 7
private const val LOCATION_PADDING = 35
private const val SHIFT = TIME_PADDING + TASK_PADDING + LEVEL_PADDING + LOCATION_PADDING
private const val MESSAGE_PADDING = 87

/**
 * A simple logging class that prints messages to the console. The class is a singleton, and all
 * messages are outputted to the console. The class has three levels of logging: DEBUG, INFO, and
 * FATAL. DEBUG is for debugging information, INFO is for general information, and FATAL is for
 * errors that will cause the program to exit. At the moment of writing, all levels are outputted to
 * the console regardless of the debug flag.
 */
class Log private constructor(header: Boolean = true) {
  private enum class Level {
    CMD,
    DEBUG,
    INFO,
    SEVERE,
    FATAL,
  }

  /**
   * The mode in which the logger will handle fatal messages. If the mode is set to EXCEPTION, the
   * logger will throw an exception. If the mode is set to EXIT, the logger will exit the program.
   */
  internal enum class FatalMode {
    EXCEPTION,
    EXIT,
  }

  /** The mode in which the logger will handle fatal messages. The default mode is set to EXIT. */
  private var fatalMode = FatalMode.EXIT

  init {
    if (header) {
      val builder = StringBuilder()
      builder.append("\u001B[1m")
      builder.append("TIME".padEnd(TIME_PADDING, ' '))
      builder.append("TASK".padEnd(TASK_PADDING, ' '))
      builder.append("LEVEL".padEnd(LEVEL_PADDING, ' '))
      builder.append("LOCATION".padEnd(LOCATION_PADDING, ' '))
      builder.append("MESSAGE".padEnd(MESSAGE_PADDING, ' '))
      builder.append("\n")
      builder.append("\u001B[0m")
      builder.append("----".padEnd(TIME_PADDING, ' '))
      builder.append("----".padEnd(TASK_PADDING, ' '))
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
  private fun output(message: String, level: Level, location: String? = null, pid: Long? = null) {
    // Get the current time.
    val instant = Date().toInstant()
    val tz = instant.atZone(TimeZone.getDefault().toZoneId())
    val iso = DateTimeFormatter.ISO_LOCAL_TIME
    val time = tz.format(iso)

    // Get the thread ID.
    val thread = (pid ?: Thread.currentThread().id).toString()

    // Get the level.
    val levelCode = level.name

    // Get the location.
    val usedLocation =
        location
            ?: run {
              val call = Throwable().stackTrace[4]
              val clazz = call.className.substringAfterLast(".").substringBefore("$")
              val method = call.methodName.substringBefore("$")
              "${clazz}::${method}::${call.lineNumber}"
            }

    // Build the message.
    val builder = StringBuilder()

    // If the message is of level debug, set color to gray.
    if (level == Level.DEBUG) {
      builder.append("\u001B[37m")
    }

    // If the message is of level command, set color to muted yellow.
    if (level == Level.CMD) {
      builder.append("\u001B[38;5;180m")
    }

    // If the message is severe, set the color to red.
    if (level == Level.SEVERE) {
      builder.append("\u001B[31m")
    }

    // Color the background red in fatal cases.
    if (level == Level.FATAL) {
      builder.append("\u001B[97m\u001B[48;5;52m")
    }

    // The actual message.
    builder.append(time.padEnd(TIME_PADDING, ' '))
    builder.append(thread.padEnd(TASK_PADDING, ' '))
    builder.append(levelCode.padEnd(LEVEL_PADDING, ' '))
    builder.append(usedLocation.padEnd(LOCATION_PADDING, ' '))
    builder.append(message)
    builder.append("\n")

    // Reset coloring.
    builder.append("\u001B[0m")

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

    when (this.fatalMode) {
      FatalMode.EXCEPTION -> throw RunnerException()
      FatalMode.EXIT -> exitProcess(1)
    }
  }

  /**
   * Print a message if and only if the debug flag is set. Note that the message will not be
   * evaluated lazily.
   *
   * @param message The message to print.
   */
  fun debug(message: String, location: String? = null) {
    output(message, Level.DEBUG, location = location)
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

  fun command(message: String, pid: Long, location: String) {
    output(message, Level.CMD, location = location, pid = pid)
  }

  internal fun setFatalMode(fatalMode: Log.FatalMode) {
    this.fatalMode = fatalMode
  }

  companion object {
    /**
     * A globally available instance of the logger. Note that at its creation, the logger will
     * output the header to the console, which is why we only allow a single instance.
     */
    val shared = Log()
  }
}
