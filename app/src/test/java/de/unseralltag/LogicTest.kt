package de.unseralltag

import de.unseralltag.data.CalendarEvent
import de.unseralltag.data.Contract
import de.unseralltag.data.NoticeUnit
import de.unseralltag.data.Recurrence
import de.unseralltag.data.SpecialDay
import de.unseralltag.data.SpecialDayType
import de.unseralltag.ui.screens.parseItem
import de.unseralltag.util.cancelDeadline
import de.unseralltag.util.describe
import de.unseralltag.util.nextDate
import de.unseralltag.util.occurrencesBetween
import de.unseralltag.util.occurrencesOnDay
import de.unseralltag.util.toMillis
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class LogicTest {
    private fun event(start: LocalDateTime, rec: String = Recurrence.NONE) =
        CalendarEvent(id = "x", title = "T", start = start.toMillis(), end = start.plusHours(1).toMillis(), recurrence = rec)

    @Test fun singleEventOnItsDayOnly() {
        val e = event(LocalDateTime.of(2026, 3, 10, 18, 0))
        assertEquals(1, occurrencesOnDay(listOf(e), LocalDate.of(2026, 3, 10)).size)
        assertEquals(0, occurrencesOnDay(listOf(e), LocalDate.of(2026, 3, 11)).size)
        assertEquals(0, occurrencesOnDay(listOf(e), LocalDate.of(2026, 3, 9)).size)
    }

    @Test fun weeklyEventRepeats() {
        val e = event(LocalDateTime.of(2026, 1, 5, 19, 0), Recurrence.WEEKLY) // Montag
        val occ = occurrencesBetween(listOf(e), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1))
        assertEquals(5, occ.size) // Montage im März 2026: 2, 9, 16, 23, 30
        assertEquals(0, occurrencesOnDay(listOf(e), LocalDate.of(2026, 1, 4)).size)
    }

    @Test fun monthlyAndYearly() {
        val m = event(LocalDateTime.of(2025, 1, 15, 9, 0), Recurrence.MONTHLY)
        assertEquals(1, occurrencesOnDay(listOf(m), LocalDate.of(2026, 7, 15)).size)
        val y = event(LocalDateTime.of(2020, 6, 1, 9, 0), Recurrence.YEARLY)
        assertEquals(1, occurrencesOnDay(listOf(y), LocalDate.of(2026, 6, 1)).size)
        assertEquals(0, occurrencesOnDay(listOf(y), LocalDate.of(2026, 6, 2)).size)
    }

    @Test fun birthdayNextDateAndAge() {
        val d = SpecialDay(name = "Anna", type = SpecialDayType.BIRTHDAY, day = 3, month = 2, year = 1990)
        val next = d.nextDate(LocalDate.of(2026, 11, 16))
        assertEquals(LocalDate.of(2027, 2, 3), next)
        assertEquals("wird 37", d.describe(next))
        assertEquals(LocalDate.of(2026, 11, 16),
            SpecialDay(day = 16, month = 11).nextDate(LocalDate.of(2026, 11, 16)))
    }

    @Test fun contractDeadline() {
        val c = Contract(endDay = LocalDate.of(2027, 3, 31).toEpochDay(), noticeValue = 3, noticeUnit = NoticeUnit.MONTHS)
        assertEquals(LocalDate.of(2026, 12, 31), c.cancelDeadline())
        assertEquals(null, Contract().cancelDeadline())
    }

    @Test fun shoppingParsing() {
        assertEquals("2" to "Milch", parseItem("2 Milch"))
        assertEquals("500g" to "Mehl", parseItem("500g Mehl"))
        assertEquals("" to "Bio Eier", parseItem("Bio Eier"))
    }
}
