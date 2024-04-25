import java.util.Date
import java.util.TimeZone
import java.util.logging.Formatter

/**
 * Pretty humanized formatting for use with the JVM logging framework.
 *
 * Included fields are:
 * - Time in ISO 8601 format.
 * - Thread ID.
 * - Log level.
 * - Class and method.
 * - Message
 *
 * Example: `2021-09-30T12:00:00.000Z [1] INFO  Main::main Hello, world!`
 */
class PrettyFormatter : Formatter() {
    override fun format(record: java.util.logging.LogRecord): String {
        // Parse date and time.
        val instant = Date(record.millis).toInstant()
        val instantTimezone = instant.atZone(TimeZone.getDefault().toZoneId())
        val isoString =
            instantTimezone.format(
                java.time.format.DateTimeFormatter.ISO_LOCAL_TIME,
            )
        val time = isoString.padEnd(12, '0')

        // Parse thread.
        val thread = "[${record.longThreadID}]".padEnd(6, ' ')
        // Parse level.
        val level = record.level.toString().padEnd(7, ' ')

        // Parse class and method.
        var className = record.sourceClassName.split(".").last()
        className =
            className.contains("$").let {
                className.split("$").first()
            }

        val location =
            "$className::${record.sourceMethodName}"
                .padEnd(30, ' ')

        // Move the message to the most right column.
        val message = if (record.message.contains("\n")) {
            "\n> " + record.message.replace("\n", "\n> ")
        } else {
            record.message
        }

        // Output the result as a single string.
        return "$time $thread $level $location $message\n"
    }
}
