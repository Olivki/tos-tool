import java.time.LocalDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField

allprojects {
    this.project.version = System.getenv("GITHUB_REF_NAME")?.substringAfter('v') ?: dateAsVersion()

    repositories {
        mavenCentral()
    }
}

fun dateAsVersion(): String {
    val formatter = DateTimeFormatterBuilder()
        .appendValue(ChronoField.YEAR, 4, 4, SignStyle.NEVER)
        .appendLiteral('.')
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendLiteral('.')
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        .appendLiteral('-')
        .appendValue(ChronoField.HOUR_OF_DAY, 2)
        .appendLiteral('.')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .appendLiteral('.')
        .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
        .toFormatter()
    return LocalDateTime.now().format(formatter)
}