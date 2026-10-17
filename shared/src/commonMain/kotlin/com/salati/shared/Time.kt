package com.salati.shared

import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds

// Calendar arithmetic and instant arithmetic are deliberately separate. All solar events are
// anchored in UTC first; the IANA zone is applied at the event instant (including DST).
@ConsistentCopyVisibility
data class LocalDate internal constructor(internal val value: kotlinx.datetime.LocalDate) {
    val year get() = value.year
    val monthValue get() = value.monthNumber
    val dayOfMonth get() = value.dayOfMonth
    val dayOfWeek get() = Weekday(value.dayOfWeek.isoDayNumber)
    fun plusDays(days: Long) = LocalDate(value.plus(days.toInt(), DateTimeUnit.DAY))
    fun minusDays(days: Long) = plusDays(-days)
    fun toEpochDay() = value.toEpochDays().toLong()
    fun atStartOfDay(zone: ZoneId) = ZonedDateTime(value.atStartOfDayIn(zone.value), zone)
    override fun toString() = value.toString()
    companion object {
        fun of(year: Int, month: Int, day: Int) = LocalDate(kotlinx.datetime.LocalDate(year, month, day))
        fun parse(iso: String) = LocalDate(kotlinx.datetime.LocalDate.parse(iso))
        fun ofEpochDay(day: Long) = LocalDate(kotlinx.datetime.LocalDate.fromEpochDays(day.toInt()))
    }
}
data class Weekday(val value: Int)
data class ZoneId(val id: String) {
    internal val value get() = TimeZone.of(id)
    companion object { fun of(id: String) = ZoneId(id).also { it.value } }
}
object ZoneOffset { val UTC = ZoneId("UTC") }
data class LocalTime(val hour: Int, val minute: Int, val second: Int = 0, val nano: Int = 0) {
    fun plusMinutes(amount: Long): LocalTime {
        val total = ((hour * 60L + minute + amount) % 1440 + 1440) % 1440
        return LocalTime((total / 60).toInt(), (total % 60).toInt(), second, nano)
    }
    fun toSecondOfDay() = hour * 3600 + minute * 60 + second
    override fun toString() = "${hour.toString().padStart(2,'0')}:${minute.toString().padStart(2,'0')}"
}
@ConsistentCopyVisibility
data class ZonedDateTime internal constructor(internal val instant: Instant, val zone: ZoneId) {
    val epochSeconds get() = instant.epochSeconds
    val nanosecond get() = instant.nanosecondsOfSecond
    fun toLocalDate() = LocalDate(instant.toLocalDateTime(zone.value).date)
    fun toLocalTime(): LocalTime = instant.toLocalDateTime(zone.value).time.let { LocalTime(it.hour,it.minute,it.second,it.nanosecond) }
    fun withZoneSameInstant(zone: ZoneId) = ZonedDateTime(instant, zone)
    fun plusMinutes(amount: Long) = ZonedDateTime(instant + amount.minutes, zone)
    fun plus(duration: Duration) = ZonedDateTime(instant + duration.nanos.nanoseconds, zone)
    fun isAfter(other: ZonedDateTime) = instant > other.instant
    fun isBefore(other: ZonedDateTime) = instant < other.instant
    companion object {
        fun now(zone: ZoneId) = ZonedDateTime(Clock.System.now(), zone)
        fun fromEpochSeconds(seconds: Long, zone: ZoneId, nano: Int = 0) = ZonedDateTime(Instant.fromEpochSeconds(seconds,nano),zone)
    }
}
@ConsistentCopyVisibility
data class Duration internal constructor(val nanos: Long) {
    val seconds get() = nanos / 1_000_000_000
    fun dividedBy(value: Long) = Duration(nanos / value)
    fun multipliedBy(value: Long) = Duration(nanos * value)
    companion object { fun between(a: ZonedDateTime, b: ZonedDateTime) = Duration((b.instant-a.instant).inWholeNanoseconds) }
}
