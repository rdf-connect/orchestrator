package technology.idlab.log

import technology.idlab.log.pretty.PrettyLog

abstract class Log(
    // The current log level. Messages with a level lower than this will not be printed.
    private val level: LogLevel
) {
  /**
   * Print a message to the output using a given level.
   *
   * @param message The message to print.
   * @param level The level of the message.
   */
  abstract fun log(message: String, level: LogLevel, location: String? = null)

  /**
   * Print a message if and only if the debug flag is set. Note that the message will not be
   * evaluated lazily.
   *
   * @param message The message to print.
   */
  fun debug(message: String) {
    log(message, LogLevel.DEBUG)
  }

  /**
   * Print a message if and only if the debug flag is set. Note that the message will be evaluated
   * lazily only if the debug flag is set.
   *
   * @param message A function determining the message to print.
   */
  fun debug(message: () -> String) {
    if (level == LogLevel.DEBUG) {
      val computedMessage = message()
      log(computedMessage, LogLevel.DEBUG)
    }
  }

  /**
   * Print a general info message. Will get outputted in all modes.
   *
   * @param message The message to print.
   */
  fun info(message: String) {
    log(message, LogLevel.INFO)
  }

  /**
   * Print a command message, which will be colored yellow in the console. The location, i.e. the
   * file and line number, will be determined by the caller.
   *
   * @param message The message to print.
   */
  fun command(location: String, message: String) {
    log(message, LogLevel.CMD, location)
  }

  /**
   * Print a severe message, which will be colored red in the console.
   *
   * @param message The message to print.
   */
  fun severe(message: String) {
    log(message, LogLevel.SEVERE)
  }

  companion object {
    /**
     * A globally available instance of the logger. Note that at its creation, the logger will
     * output the header to the console, which is why we only allow a single instance.
     */
    val shared = PrettyLog(LogLevel.INFO)
  }
}
