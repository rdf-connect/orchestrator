package technology.idlab.rdfc.core.log

import technology.idlab.rdfc.core.log.pretty.PrettyLog

/**
 * Logging in the orchestrator can be implemented very broadly. We define a very simple interface
 * which allows messages to be submitted at a variety of levels. Actual implementations may be as
 * simple as printing to the console or as complex as sending messages to a remote server.
 *
 * @property level The log level. Messages with a level lower than this will not be printed.
 */
abstract class Log(private val level: LogLevel) {
  /**
   * Print a message to the output using a given level. This is the only method that must be
   * implemented by subclasses, but it should not be called directly by clients.
   *
   * @param message The message to print.
   * @param level The level of the message.
   */
  abstract fun output(message: String, level: LogLevel, location: String? = null)

  /**
   * Print a message if and only if the debug flag is set. Note that the message will not be
   * evaluated lazily.
   *
   * @param message The message to print.
   */
  fun debug(message: String) {
    output(message, LogLevel.DEBUG)
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
      output(computedMessage, LogLevel.DEBUG)
    }
  }

  /**
   * Print a general info message. Will get outputted in all modes.
   *
   * @param message The message to print.
   */
  fun info(message: String) {
    output(message, LogLevel.INFO)
  }

  /**
   * Print a command message, which will be colored yellow in the console. The location, i.e. the
   * file and line number, will be determined by the caller.
   *
   * @param message The message to print.
   */
  fun command(location: String, message: String) {
    output(message, LogLevel.CMD, location)
  }

  /**
   * Print a severe message, which will be colored red in the console.
   *
   * @param message The message to print.
   */
  fun severe(message: String) {
    output(message, LogLevel.SEVERE)
  }

  companion object {
    /**
     * A globally available instance of the logger. Note that at its creation, the logger will
     * output the header to the console, which is why we only allow a single instance.
     */
    val shared = PrettyLog(LogLevel.DEBUG)
  }
}
