package technology.idlab.log.pretty

import java.time.format.DateTimeFormatter
import java.util.*
import technology.idlab.log.Log
import technology.idlab.log.LogLevel

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
class PrettyLog(level: LogLevel) : Log(level) {
  init {
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

  override fun log(message: String, level: LogLevel) {
    // Get the current time.
    val instant = Date().toInstant()
    val tz = instant.atZone(TimeZone.getDefault().toZoneId())
    val iso = DateTimeFormatter.ISO_LOCAL_TIME

    val time = tz.format(iso)
    val levelCode = level.name

    val stack = Throwable().stackTrace[4]
    val file = "${stack.fileName ?: "Unknown"}:${stack.lineNumber}"

    // Build the message.
    val builder = StringBuilder()

    // If the message is of level debug, set color to gray.
    if (level == LogLevel.DEBUG) {
      builder.append("\u001B[37m")
    }

    // If the message is of level command, set color to muted yellow.
    if (level == LogLevel.CMD) {
      builder.append("\u001B[38;5;180m")
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
}
