package de.unseralltag.util

import de.unseralltag.data.CalendarEvent
import de.unseralltag.data.Contract
import de.unseralltag.data.NoticeUnit
import de.unseralltag.data.Recurrence
import de.unseralltag.data.SpecialDay
import de.unseralltag.data.SpecialDayType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

val GERMAN: Locale = Locale.GERMANY
val zone: ZoneId get() = ZoneId.systemDefault()

fun today(): LocalDate = LocalDate.now(zone)
fun Long.toLocalDate(): LocalDate = LocalDate.ofEpochDay(this)
fun Long.millisToDateTime(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), zone)
fun Long.millisToDate(): LocalDate = millisToDateTime().toLocalDate()
fun LocalDateTime.toMillis(): Long = atZone(zone).toInstant().toEpochMilli()
fun LocalDate.startMillis(): Long = atStartOfDay(zone).toInstant().toEpochMilli()

/** Material3-DatePicker liefert UTC-Mitternacht. */
fun utcMillisToLocalDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private val dateFmt = DateTimeFormatter.ofPattern("EEE, d. MMM yyyy", GERMAN)
private val shortDateFmt = DateTimeFormatter.ofPattern("EEE, d. MMM", GERMAN)
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", GERMAN)
private val numDateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy", GERMAN)

fun LocalDate.formatLong(): String = format(dateFmt)
fun LocalDate.formatShort(): String = format(shortDateFmt)
fun LocalDate.formatNumeric(): String = format(numDateFmt)
fun LocalTime.formatTime(): String = format(timeFmt)
fun LocalDateTime.formatTime(): String = format(timeFmt)
fun monthName(month: Int): String =
    java.time.Month.of(month).getDisplayName(TextStyle.FULL, GERMAN)

/** "Heute", "Morgen", "in 3 Tagen", ... */
fun relativeDay(date: LocalDate, from: LocalDate = today()): String {
    val d = ChronoUnit.DAYS.between(from, date)
    return when {
        d == 0L -> "Heute"
        d == 1L -> "Morgen"
        d == 2L -> "Übermorgen"
        d == -1L -> "Gestern"
        d < 0 -> "vor ${-d} Tagen"
        d < 7 -> date.dayOfWeek.getDisplayName(TextStyle.FULL, GERMAN)
        else -> "in $d Tagen"
    }
}

/* ---------- Termine ---------- */

data class Occurrence(val event: CalendarEvent, val start: LocalDateTime, val end: LocalDateTime)

private fun step(dt: ZonedDateTime, rec: String, n: Long): ZonedDateTime = when (rec) {
    Recurrence.DAILY -> dt.plusDays(n)
    Recurrence.WEEKLY -> dt.plusWeeks(n)
    Recurrence.MONTHLY -> dt.plusMonths(n)
    Recurrence.YEARLY -> dt.plusYears(n)
    else -> dt
}

/** Alle Vorkommen eines Termins, die sich mit [from, to) überschneiden. */
fun occurrences(e: CalendarEvent, from: LocalDateTime, to: LocalDateTime): List<Occurrence> {
    val start = Instant.ofEpochMilli(e.start).atZone(zone)
    val duration = (e.end - e.start).coerceAtLeast(0)
    val fromZ = from.atZone(zone)
    val toZ = to.atZone(zone)
    val out = mutableListOf<Occurrence>()
    if (e.recurrence == Recurrence.NONE || e.recurrence.isBlank()) {
        val end = start.plusNanos(duration * 1_000_000)
        if (start.isBefore(toZ) && (end.isAfter(fromZ) || !start.isBefore(fromZ))) {
            out += Occurrence(e, start.toLocalDateTime(), end.toLocalDateTime())
        }
        return out
    }
    // Schnell zum ersten relevanten Vorkommen springen
    var n = 0L
    if (start.isBefore(fromZ)) {
        val approx = when (e.recurrence) {
            Recurrence.DAILY -> ChronoUnit.DAYS.between(start, fromZ)
            Recurrence.WEEKLY -> ChronoUnit.WEEKS.between(start, fromZ)
            Recurrence.MONTHLY -> ChronoUnit.MONTHS.between(start, fromZ)
            Recurrence.YEARLY -> ChronoUnit.YEARS.between(start, fromZ)
            else -> 0L
        }
        n = (approx - 1).coerceAtLeast(0)
    }
    var guard = 0
    while (guard++ < 2000) {
        val s = step(start, e.recurrence, n)
        if (!s.isBefore(toZ)) break
        val en = s.plusNanos(duration * 1_000_000)
        if (en.isAfter(fromZ) || !s.isBefore(fromZ)) {
            out += Occurrence(e, s.toLocalDateTime(), en.toLocalDateTime())
        }
        n++
    }
    return out
}

fun occurrencesOnDay(events: List<CalendarEvent>, day: LocalDate): List<Occurrence> =
    events.flatMap { occurrences(it, day.atStartOfDay(), day.plusDays(1).atStartOfDay()) }
        .sortedWith(compareBy({ !it.event.allDay }, { it.start }))

fun occurrencesBetween(events: List<CalendarEvent>, from: LocalDate, toExclusive: LocalDate): List<Occurrence> =
    events.flatMap { occurrences(it, from.atStartOfDay(), toExclusive.atStartOfDay()) }
        .sortedBy { it.start }

fun Occurrence.timeLabel(): String =
    if (event.allDay) "Ganztägig" else "${start.formatTime()} – ${end.formatTime()}"

/* ---------- Besondere Tage ---------- */

fun SpecialDay.nextDate(from: LocalDate = today()): LocalDate {
    val md = runCatching { MonthDay.of(month, day) }.getOrElse { MonthDay.of(1, 1) }
    var d = md.atYear(from.year)
    if (d.isBefore(from)) d = md.atYear(from.year + 1)
    return d
}

/** Alter bzw. Anzahl Jahre am nächsten Termin, null wenn Jahr unbekannt. */
fun SpecialDay.yearsAt(date: LocalDate): Int? = if (year > 0) date.year - year else null

fun SpecialDay.describe(date: LocalDate = nextDate()): String {
    val y = yearsAt(date)
    return when (type) {
        SpecialDayType.BIRTHDAY -> if (y != null) "wird $y" else "Geburtstag"
        SpecialDayType.ANNIVERSARY -> if (y != null) "$y. Jahrestag" else "Jahrestag"
        else -> if (y != null) "$y Jahre" else ""
    }
}

/* ---------- Verträge ---------- */

/** Letzter Tag, an dem noch gekündigt werden kann. */
fun Contract.cancelDeadline(): LocalDate? {
    val end = endDay?.toLocalDate() ?: return null
    return when (noticeUnit) {
        NoticeUnit.DAYS -> end.minusDays(noticeValue.toLong())
        NoticeUnit.WEEKS -> end.minusWeeks(noticeValue.toLong())
        else -> end.minusMonths(noticeValue.toLong())
    }
}

fun Contract.monthlyCost(): Double = when (costInterval) {
    de.unseralltag.data.CostInterval.MONTHLY -> cost
    de.unseralltag.data.CostInterval.QUARTERLY -> cost / 3
    de.unseralltag.data.CostInterval.YEARLY -> cost / 12
    else -> 0.0
}

fun formatEuro(v: Double): String = String.format(GERMAN, "%,.2f €", v)

fun daysUntil(date: LocalDate, from: LocalDate = today()): Long = ChronoUnit.DAYS.between(from, date)
