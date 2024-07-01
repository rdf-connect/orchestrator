package technology.idlab.util

import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.TimeZone
import kotlin.Exception
import technology.idlab.exception.RunnerException

class Log private constructor() {
  enum class Cause(val message: String) {
    JVM_RUNNER_STAGE_NO_CLASS("The processor has no class key set."),
  }

  enum class Level {
    INFO,
    SEVERE,
    FATAL,
    DEBUG,
    ;

    fun style(string: String): String {
      return when (this) {
        INFO -> string
        SEVERE -> string
        FATAL -> string
        DEBUG -> "\u001B[34m${string}\u001B[0m"
      }
    }

    fun code(): String {
      return when (this) {
        INFO -> "INFO"
        SEVERE -> "SEVERE"
        FATAL -> "FATAL"
        DEBUG -> "DEBUG"
      }
    }
  }

  init {
    val header =
        listOf(
                "TIME".padEnd(12, ' '),
                "THREAD".padEnd(6, ' '),
                "LEVEL".padEnd(7, ' '),
                "LOCATION".padEnd(50, ' '),
                "MESSAGE",
            )
            .joinToString(" ")
    println(header)

    val separator =
        listOf(
                "----".padEnd(12, ' '),
                "------".padEnd(6, ' '),
                "-----".padEnd(7, ' '),
                "--------".padEnd(50, ' '),
                "-------",
            )
            .joinToString(" ")
    println(separator)
  }

  private fun line(message: String, level: Level): String {
    val instant = Date().toInstant()
    val tz = instant.atZone(TimeZone.getDefault().toZoneId())
    val iso = DateTimeFormatter.ISO_LOCAL_TIME
    val time = tz.format(iso)

    val caller = Throwable().stackTrace[3]
    val name =
        "${caller.className.substringAfterLast(".")}::${caller.methodName}::${caller.lineNumber}"

    return listOf(
            time.padEnd(12, '0'),
            "[${Thread.currentThread().id}]".padEnd(6, ' '),
            level.code().padEnd(7, ' '),
            name.padEnd(50, ' '),
            message,
        )
        .joinToString(" ")
  }

  private fun toConsole(message: String, level: Level) {
    println(level.style(line(message, level)))
  }

  fun info(message: String) {
    toConsole(message, Level.INFO)
  }

  fun severe(message: String) {
    toConsole(message, Level.SEVERE)
  }

  fun fatal(message: String): Nothing {
    toConsole(message, Level.FATAL)
    throw RunnerException()
  }

  fun fatal(exception: Exception): Nothing {
    toConsole(exception.message.toString(), Level.FATAL)
    throw RunnerException()
  }

  fun fatal(message: String, exception: Exception) {
    toConsole("$message - ${exception.message}", Level.FATAL)
    throw RunnerException()
  }

  fun fatal(cause: Cause): Nothing = fatal(cause.message)

  fun debug(message: String) {
    toConsole(message, Level.DEBUG)
  }

  fun assert(condition: Boolean, message: String) {
    if (!condition) {
      fatal(message)
    }
  }

  fun runtime(runtime: String, message: String) {
    val instant = Date().toInstant()
    val tz = instant.atZone(TimeZone.getDefault().toZoneId())
    val iso = DateTimeFormatter.ISO_LOCAL_TIME
    val time = tz.format(iso)

    val line =
        listOf(
                time.padEnd(12, '0'),
                "[${Thread.currentThread().id}]".padEnd(6, ' '),
                "INFO".padEnd(7, ' '),
                runtime.padEnd(50, ' '),
                message,
            )
            .joinToString(" ")

    println(line)
  }

  fun runtimeFatal(runtime: String, message: String): Nothing {
    val instant = Date().toInstant()
    val tz = instant.atZone(TimeZone.getDefault().toZoneId())
    val iso = DateTimeFormatter.ISO_LOCAL_TIME
    val time = tz.format(iso)

    val line =
        listOf(
                time.padEnd(12, '0'),
                "[${Thread.currentThread().id}]".padEnd(6, ' '),
                "FATAL".padEnd(7, ' '),
                runtime.padEnd(50, ' '),
                message,
            )
            .joinToString(" ")

    println(line)
    throw RunnerException()
  }

  companion object {
    val shared = Log()
  }
}
