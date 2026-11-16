package de.unseralltag.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import de.unseralltag.data.CalendarEvent
import de.unseralltag.data.Collections
import de.unseralltag.data.Contract
import de.unseralltag.data.Meal
import de.unseralltag.data.ShoppingItem
import de.unseralltag.data.SpecialDay
import de.unseralltag.data.TaskItem
import de.unseralltag.ui.LocalSession
import de.unseralltag.ui.Nav
import de.unseralltag.ui.Routes
import de.unseralltag.ui.ScreenScaffold
import de.unseralltag.ui.common.ColorDot
import de.unseralltag.ui.common.collection
import de.unseralltag.util.cancelDeadline
import de.unseralltag.util.daysUntil
import de.unseralltag.util.describe
import de.unseralltag.util.formatLong
import de.unseralltag.util.formatShort
import de.unseralltag.util.nextDate
import de.unseralltag.util.occurrencesBetween
import de.unseralltag.util.relativeDay
import de.unseralltag.util.timeLabel
import de.unseralltag.util.toLocalDate
import de.unseralltag.util.today
import java.time.LocalTime

@Composable
fun TodayScreen(nav: Nav) {
    val session = LocalSession.current
    val events by collection<CalendarEvent>(Collections.EVENTS)
    val specialDays by collection<SpecialDay>(Collections.SPECIAL_DAYS)
    val tasks by collection<TaskItem>(Collections.TASKS)
    val meals by collection<Meal>(Collections.MEALS)
    val contracts by collection<Contract>(Collections.CONTRACTS)
    val shopping by collection<ShoppingItem>(Collections.SHOPPING_ITEMS)
    val today = remember { today() }

    val greeting = when (LocalTime.now().hour) {
        in 5..10 -> "Guten Morgen"
        in 11..17 -> "Hallo"
        else -> "Guten Abend"
    }

    val upcoming = remember(events) { occurrencesBetween(events, today, today.plusDays(3)) }
    val days = remember(specialDays) {
        specialDays.map { it to it.nextDate(today) }
            .filter { daysUntil(it.second, today) <= 30 }
            .sortedBy { it.second }
    }
    val dueTasks = remember(tasks) {
        tasks.filter { !it.done && it.dueDay != null && it.dueDay!! <= today.toEpochDay() }
            .sortedBy { it.dueDay }
    }
    val todaysMeals = remember(meals) { meals.filter { it.day == today.toEpochDay() } }
    val deadlines = remember(contracts) {
        contracts.mapNotNull { c -> c.cancelDeadline()?.let { c to it } }
            .filter { daysUntil(it.second, today) in 0..30 }
            .sortedBy { it.second }
    }
    val openItems = shopping.count { !it.checked }

    ScreenScaffold(
        title = "$greeting, ${session.myName}",
        actions = {
            IconButton(onClick = { nav.go(Routes.SETTINGS) }) { Icon(Icons.Default.Settings, "Einstellungen") }
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(today.formatLong(), style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                TodayCard("Termine", Icons.Default.CalendarMonth, { nav.go(Routes.CALENDAR) }) {
                    if (upcoming.isEmpty()) Text("Keine Termine in den nächsten Tagen 🎉")
                    upcoming.take(6).forEach { o ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ColorDot(session.colorOf(o.event.assignedTo))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(o.event.title, style = MaterialTheme.typography.bodyLarge)
                                Text("${relativeDay(o.start.toLocalDate(), today)} · ${o.timeLabel()}",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            if (days.isNotEmpty()) item {
                TodayCard("Besondere Tage", Icons.Default.Cake, { nav.go(Routes.SPECIAL_DAYS) }) {
                    days.take(5).forEach { (d, date) ->
                        Text("${d.name} – ${d.describe(date)}", style = MaterialTheme.typography.bodyLarge)
                        Text("${relativeDay(date, today)} (${date.formatShort()})", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                TodayCard("Fällige Aufgaben", Icons.Default.CheckCircle, { nav.go(Routes.TASKS) }) {
                    if (dueTasks.isEmpty()) Text("Alles erledigt ✔")
                    dueTasks.take(6).forEach { t ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ColorDot(session.colorOf(t.assignedTo))
                            Spacer(Modifier.width(8.dp))
                            Text(t.title, Modifier.weight(1f))
                            val due = t.dueDay!!.toLocalDate()
                            Text(relativeDay(due, today), style = MaterialTheme.typography.bodySmall,
                                color = if (due < today) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                TodayCard("Essen heute", Icons.Default.Restaurant, { nav.go(Routes.MEALS) }) {
                    if (todaysMeals.isEmpty()) Text("Noch nichts geplant") else todaysMeals.forEach { Text(it.title) }
                }
            }
            item {
                TodayCard("Einkauf", Icons.Default.ShoppingCart, { nav.go(Routes.SHOPPING) }) {
                    Text(if (openItems == 0) "Nichts zu besorgen" else "$openItems Artikel offen")
                }
            }
            if (deadlines.isNotEmpty()) item {
                TodayCard("Kündigungsfristen", Icons.Default.Description, { nav.go(Routes.CONTRACTS) }) {
                    deadlines.forEach { (c, d) ->
                        Text("${c.name}: kündigen bis ${d.formatShort()} (${relativeDay(d, today)})",
                            color = if (daysUntil(d, today) <= 7) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            item { Spacer(Modifier.padding(8.dp)) }
        }
    }
}

@Composable
private fun TodayCard(title: String, icon: ImageVector, onClick: () -> Unit, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}
