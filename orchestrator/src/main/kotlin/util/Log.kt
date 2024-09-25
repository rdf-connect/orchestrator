package technology.idlab.util

import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.system.exitProcess
import technology.idlab.exception.UnknownException

private const val TIME_PADDING = 15
private const val LEVEL_PADDING = 10
private const val FILE_PADDING = 39
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
      builder.append("LEVEL".padEnd(LEVEL_PADDING, ' '))
      builder.append("FILE".padEnd(FILE_PADDING, ' '))
      builder.append("MESSAGE".padEnd(MESSAGE_PADDING, ' '))
      builder.append("\n")
      builder.append("\u001B[0m")
      builder.append("----".padEnd(TIME_PADDING, ' '))
      builder.append("-----".padEnd(LEVEL_PADDING, ' '))
      builder.append("----".padEnd(FILE_PADDING, ' '))
      builder.append("-------\n")

      synchronized(System.out) { print(builder) }
    }
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
    val levelCode = level.name

    val file =
        location
            ?: run {
              val stack = Throwable().stackTrace[4]
              "${stack.fileName ?: "Unknown"}:${stack.lineNumber}"
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
    if (level == Level.FATAL) {
      builder.append("\u001B[31m")
    }

    // The actual message.
    builder.append(time.padEnd(TIME_PADDING, ' '))
    builder.append(levelCode.padEnd(LEVEL_PADDING, ' '))
    builder.append(file.padEnd(FILE_PADDING, ' '))
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
      FatalMode.EXCEPTION -> throw UnknownException()
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

  /**
   * Print a command message, which will be colored yellow in the console. The location, i.e. the
   * file and line number, will be determined by the caller.
   *
   * @param message The message to print.
   */
  fun command(message: String, location: String) {
    output(message, Level.CMD, location = location)
  }

  /**
   * When this method is called with `false`, the logger will throw an exception instead of exiting
   * the program. This is useful for testing purposes and should not be called during normal
   * operation.
   */
  internal fun setFatalMode(fatalMode: FatalMode) {
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
