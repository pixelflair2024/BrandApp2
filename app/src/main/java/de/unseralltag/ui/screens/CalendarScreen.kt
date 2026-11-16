package de.unseralltag.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.unseralltag.data.CalendarEvent
import de.unseralltag.data.Collections
import de.unseralltag.data.Recurrence
import de.unseralltag.data.SpecialDay
import de.unseralltag.data.TaskItem
import de.unseralltag.ui.LocalSession
import de.unseralltag.ui.Nav
import de.unseralltag.ui.ScreenScaffold
import de.unseralltag.ui.common.ColorDot
import de.unseralltag.ui.common.DateField
import de.unseralltag.ui.common.EditorDialog
import de.unseralltag.ui.common.EmptyState
import de.unseralltag.ui.common.OptionPicker
import de.unseralltag.ui.common.PersonSelector
import de.unseralltag.ui.common.TimeField
import de.unseralltag.ui.common.collection
import de.unseralltag.util.GERMAN
import de.unseralltag.util.describe
import de.unseralltag.util.formatLong
import de.unseralltag.util.millisToDateTime
import de.unseralltag.util.monthName
import de.unseralltag.util.nextDate
import de.unseralltag.util.occurrencesBetween
import de.unseralltag.util.occurrencesOnDay
import de.unseralltag.util.timeLabel
import de.unseralltag.util.toMillis
import de.unseralltag.util.today
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle

@Composable
fun CalendarScreen(nav: Nav) {
    val session = LocalSession.current
    val events by collection<CalendarEvent>(Collections.EVENTS)
    val specialDays by collection<SpecialDay>(Collections.SPECIAL_DAYS)
    val tasks by collection<TaskItem>(Collections.TASKS)
    var monthStr by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthStr)
    var selectedStr by rememberSaveable { mutableStateOf(today().toString()) }
    val selected = LocalDate.parse(selectedStr)
    var editing by remember { mutableStateOf<CalendarEvent?>(null) }

    val first = month.atDay(1)
    val gridStart = first.minusDays((first.dayOfWeek.value - 1).toLong())
    val gridEnd = gridStart.plusDays(42)
    val monthOccurrences = remember(events, monthStr) { occurrencesBetween(events, gridStart, gridEnd) }
    val busyDays = remember(monthOccurrences) {
        monthOccurrences.groupBy { it.start.toLocalDate() }.mapValues { e -> e.value.map { it.event.assignedTo }.distinct() }
    }
    val specialByDay = remember(specialDays, monthStr) {
        specialDays.flatMap { d -> listOf(d.nextDate(gridStart), d.nextDate(gridStart.plusDays(41))).distinct().map { it to d } }
            .groupBy({ it.first }, { it.second })
    }

    ScreenScaffold(
        title = "Kalender",
        onAdd = {
            val s = selected.atTime(LocalTime.now().plusHours(1).withMinute(0))
            editing = CalendarEvent(start = s.toMillis(), end = s.plusHours(1).toMillis())
        },
        actions = {
            IconButton(onClick = { monthStr = YearMonth.now().toString(); selectedStr = today().toString() }) {
                Icon(Icons.Default.Today, "Heute")
            }
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad)) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { monthStr = month.minusMonths(1).toString() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Vorheriger Monat")
                    }
                    Text("${monthName(month.monthValue)} ${month.year}", Modifier.weight(1f),
                        textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { monthStr = month.plusMonths(1).toString() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Nächster Monat")
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 8.dp)) {
                    Row {
                        DayOfWeek.entries.forEach {
                            Text(it.getDisplayName(TextStyle.SHORT, GERMAN), Modifier.weight(1f),
                                textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    for (w in 0 until 6) {
                        Row {
                            for (d in 0 until 7) {
                                val day = gridStart.plusDays((w * 7 + d).toLong())
                                DayCell(
                                    day = day,
                                    inMonth = day.month == month.month,
                                    selected = day == selected,
                                    isToday = day == today(),
                                    who = busyDays[day].orEmpty(),
                                    special = specialByDay[day] != null,
                                    modifier = Modifier.weight(1f),
                                    onClick = { selectedStr = day.toString() },
                                )
                            }
                        }
                    }
                }
            }
            item {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(selected.formatLong(), Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleMedium)
            }
            val dayOcc = occurrencesOnDay(events, selected)
            val daySpecial = specialDays.filter { it.nextDate(selected) == selected }
            val dayTasks = tasks.filter { it.dueDay == selected.toEpochDay() }
            items(daySpecial, key = { "s" + it.id }) { d ->
                ListItem(
                    headlineContent = { Text(d.name) },
                    supportingContent = { Text(d.describe(selected)) },
                    leadingContent = { Icon(Icons.Default.Cake, null, tint = MaterialTheme.colorScheme.tertiary) },
                )
            }
            items(dayOcc, key = { "e" + it.event.id + it.start }) { o ->
                ListItem(
                    modifier = Modifier.clickable { editing = o.event },
                    headlineContent = { Text(o.event.title) },
                    supportingContent = {
                        Text(listOf(o.timeLabel(), o.event.location, session.nameOf(o.event.assignedTo))
                            .filter { it.isNotBlank() }.joinToString(" · "))
                    },
                    leadingContent = { ColorDot(session.colorOf(o.event.assignedTo), 14) },
                )
            }
            items(dayTasks, key = { "t" + it.id }) { t ->
                ListItem(
                    headlineContent = { Text(t.title) },
                    supportingContent = { Text("Aufgabe · ${session.nameOf(t.assignedTo)}") },
                    leadingContent = {
                        Checkbox(t.done, { session.store.update(Collections.TASKS, t.id, mapOf("done" to it)) })
                    },
                )
            }
            if (dayOcc.isEmpty() && daySpecial.isEmpty() && dayTasks.isEmpty()) {
                item { EmptyState(Icons.Default.EventBusy, "Keine Einträge an diesem Tag") }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    editing?.let { e -> EventEditor(e) { editing = null } }
}

@Composable
private fun DayCell(
    day: LocalDate, inMonth: Boolean, selected: Boolean, isToday: Boolean,
    who: List<String>, special: Boolean, modifier: Modifier, onClick: () -> Unit,
) {
    val session = LocalSession.current
    val cs = MaterialTheme.colorScheme
    Box(
        modifier.aspectRatio(1f).padding(2.dp).clip(CircleShape)
            .background(if (selected) cs.primaryContainer else cs.surface)
            .then(if (isToday) Modifier.border(2.dp, cs.primary, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                day.dayOfMonth.toString(),
                color = if (inMonth) cs.onSurface else cs.outline,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                who.take(3).forEach { ColorDot(session.colorOf(it), 5) }
                if (special) ColorDot(cs.tertiary, 5)
            }
        }
    }
}

private val reminderOptions = linkedMapOf(
    -1 to "Keine",
    0 to "Zum Beginn",
    15 to "15 Min. vorher",
    30 to "30 Min. vorher",
    60 to "1 Std. vorher",
    120 to "2 Std. vorher",
    1440 to "1 Tag vorher",
    2880 to "2 Tage vorher",
    10080 to "1 Woche vorher",
)

@Composable
fun EventEditor(initial: CalendarEvent, onDismiss: () -> Unit) {
    val session = LocalSession.current
    var title by remember { mutableStateOf(initial.title) }
    var location by remember { mutableStateOf(initial.location) }
    var notes by remember { mutableStateOf(initial.notes) }
    var allDay by remember { mutableStateOf(initial.allDay) }
    val s0 = initial.start.millisToDateTime()
    val e0 = initial.end.millisToDateTime()
    var startDate by remember { mutableStateOf(s0.toLocalDate()) }
    var startTime by remember { mutableStateOf(s0.toLocalTime()) }
    var endDate by remember { mutableStateOf(e0.toLocalDate()) }
    var endTime by remember { mutableStateOf(e0.toLocalTime()) }
    var reminder by remember { mutableStateOf(if (initial.reminderMinutes in reminderOptions) initial.reminderMinutes else 60) }
    var assigned by remember { mutableStateOf(initial.assignedTo) }
    var recurrence by remember { mutableStateOf(initial.recurrence) }

    EditorDialog(
        title = if (initial.id.isBlank()) "Neuer Termin" else "Termin bearbeiten",
        onDismiss = onDismiss,
        saveEnabled = title.isNotBlank(),
        onDelete = if (initial.id.isBlank()) null else ({ session.store.delete(Collections.EVENTS, initial.id) }),
        onSave = {
            var start = if (allDay) startDate.atStartOfDay() else startDate.atTime(startTime)
            var end = if (allDay) endDate.plusDays(1).atStartOfDay().minusMinutes(1) else endDate.atTime(endTime)
            if (end.isBefore(start)) end = start.plusHours(1)
            session.store.save(
                Collections.EVENTS, initial.id,
                initial.copy(
                    title = title.trim(), location = location.trim(), notes = notes.trim(), allDay = allDay,
                    start = start.toMillis(), end = end.toMillis(), reminderMinutes = reminder,
                    assignedTo = assigned, recurrence = recurrence,
                    createdBy = initial.createdBy.ifBlank { session.myUid },
                ),
            )
        },
    ) {
        OutlinedTextField(title, { title = it }, label = { Text("Titel") }, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ganztägig", Modifier.weight(1f)); Switch(allDay, { allDay = it })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DateField("Beginn", startDate, {
                it?.let { d ->
                    val diff = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate)
                    startDate = d; endDate = d.plusDays(diff)
                }
            })
            if (!allDay) TimeField("Uhrzeit", startTime, {
                val dur = java.time.Duration.between(startDate.atTime(startTime), endDate.atTime(endTime))
                startTime = it
                val newEnd = startDate.atTime(it).plus(dur)
                endDate = newEnd.toLocalDate(); endTime = newEnd.toLocalTime()
            })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DateField("Ende", endDate, { it?.let { d -> endDate = d } })
            if (!allDay) TimeField("Uhrzeit", endTime, { endTime = it })
        }
        PersonSelector(assigned, { assigned = it })
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OptionPicker("Erinnerung", reminderOptions, reminder, { reminder = it })
            OptionPicker("Wiederholen", Recurrence.labels, recurrence, { recurrence = it })
        }
        OutlinedTextField(location, { location = it }, label = { Text("Ort") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(notes, { notes = it }, label = { Text("Notizen") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.width(1.dp))
    }
}
