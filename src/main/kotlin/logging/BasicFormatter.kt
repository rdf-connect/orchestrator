import java.util.*
import java.util.logging.Formatter

class BasicFormatter(): Formatter() {
    override fun format(record: java.util.logging.LogRecord): String {
        // Parse date and time.
        val instant = Date(record.millis).toInstant()
        val instantTimezone = instant.atZone(TimeZone.getDefault().toZoneId())
        val isoString = instantTimezone.format(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME)
        val time = isoString.padEnd(12, '0');

        // Parse thread.
        val thread = "[${record.longThreadID}]".padEnd(6, ' ');
        // Parse level.
        val level = record.level.toString().padEnd(7, ' ');

        // Parse class and method.
        val className = record.sourceClassName.split(".").last();
        val location = "${className}::${record.sourceMethodName}"
            .padEnd(30, ' ');

        // Output the result as a single string.
        return "$time $thread $level $location ${record.message}\n"
    }
}
