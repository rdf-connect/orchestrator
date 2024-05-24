package technology.idlab.logging

import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.TimeZone
import kotlin.Exception
import kotlin.system.exitProcess

class Log private constructor() {
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

  private fun print(message: String, level: String) {
    val instant = Date().toInstant()
    val tz = instant.atZone(TimeZone.getDefault().toZoneId())
    val iso = DateTimeFormatter.ISO_LOCAL_TIME
    val time = tz.format(iso)

    val caller = Throwable().stackTrace[2]
    val name =
        "${caller.className.substringAfterLast(".")}::${caller.methodName}::${caller.lineNumber}"

    val line =
        listOf(
                time.padEnd(12, '0'),
                "[${Thread.currentThread().id}]".padEnd(6, ' '),
                level.padEnd(7, ' '),
                name.padEnd(50, ' '),
                message,
            )
            .joinToString(" ")

    println(line)
  }

  fun info(message: String) {
    print(message, "INFO")
  }

  fun severe(message: String) {
    print(message, "SEVERE")
  }

  fun fatal(message: String): Nothing {
    print(message, "FATAL")
    print(Throwable().stackTraceToString())
    exitProcess(1)
  }

  fun fatal(exception: Exception): Nothing {
    print(exception.message.toString(), "FATAL")
    print(Throwable().stackTraceToString())
    exitProcess(1)
  }

  fun fatal(message: String, exception: Exception) {
    print("$message - ${exception.message}")
    print(Throwable().stackTraceToString())
    exitProcess(1)
  }

  fun debug(message: String) {
    print(message, "DEBUG")
  }

  fun assert(condition: Boolean, message: String) {
    if (!condition) {
      fatal(message)
    }
  }

  companion object {
    val shared = Log()
  }
}
