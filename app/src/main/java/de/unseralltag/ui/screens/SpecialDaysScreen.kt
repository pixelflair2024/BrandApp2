package de.unseralltag.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ListItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.unseralltag.data.Collections
import de.unseralltag.data.SpecialDay
import de.unseralltag.data.SpecialDayType
import de.unseralltag.ui.LocalSession
import de.unseralltag.ui.Nav
import de.unseralltag.ui.ScreenScaffold
import de.unseralltag.ui.common.EditorDialog
import de.unseralltag.ui.common.EmptyState
import de.unseralltag.ui.common.OptionPicker
import de.unseralltag.ui.common.collection
import de.unseralltag.util.daysUntil
import de.unseralltag.util.describe
import de.unseralltag.util.formatShort
import de.unseralltag.util.monthName
import de.unseralltag.util.nextDate
import de.unseralltag.util.relativeDay
import de.unseralltag.util.today
import java.time.YearMonth

@Composable
fun SpecialDaysScreen(nav: Nav) {
    val days by collection<SpecialDay>(Collections.SPECIAL_DAYS)
    var editing by remember { mutableStateOf<SpecialDay?>(null) }
    val today = today()
    val sorted = remember(days) { days.map { it to it.nextDate(today) }.sortedBy { it.second } }

    ScreenScaffold("Geburtstage & Jahrestage", onBack = nav::back, onAdd = { editing = SpecialDay() }) { pad ->
        if (sorted.isEmpty()) {
            EmptyState(Icons.Default.Cake, "Noch keine Geburtstage oder Jahrestage.\nTippe auf +, um einen hinzuzufügen.",
                Modifier.padding(pad))
        }
        LazyColumn(Modifier.fillMaxSize().padding(pad)) {
            items(sorted, key = { it.first.id }) { (d, date) ->
                val left = daysUntil(date, today)
                ListItem(
                    modifier = Modifier.clickable { editing = d },
                    leadingContent = {
                        Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                            contentAlignment = Alignment.Center) {
                            Icon(iconFor(d.type), null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    },
                    headlineContent = { Text(d.name) },
                    supportingContent = {
                        Text(listOf(date.formatShort(), d.describe(date)).filter { it.isNotBlank() }.joinToString(" · "))
                    },
                    trailingContent = {
                        Text(
                            relativeDay(date, today),
                            color = if (left <= 7) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    editing?.let { SpecialDayEditor(it) { editing = null } }
}

private fun iconFor(type: String) = when (type) {
    SpecialDayType.BIRTHDAY -> Icons.Default.Cake
    SpecialDayType.ANNIVERSARY -> Icons.Default.Favorite
    else -> Icons.Default.Celebration
}

private val reminderDayOptions = linkedMapOf(-1 to "Nur am Tag", 1 to "1 Tag vorher", 3 to "3 Tage vorher",
    7 to "1 Woche vorher", 14 to "2 Wochen vorher", 30 to "1 Monat vorher")

@Composable
private fun SpecialDayEditor(initial: SpecialDay, onDismiss: () -> Unit) {
    val session = LocalSession.current
    var name by remember { mutableStateOf(initial.name) }
    var type by remember { mutableStateOf(initial.type) }
    var day by remember { mutableStateOf(initial.day) }
    var month by remember { mutableStateOf(initial.month) }
    var yearText by remember { mutableStateOf(if (initial.year > 0) initial.year.toString() else "") }
    var notes by remember { mutableStateOf(initial.notes) }
    var reminder by remember { mutableStateOf(if (initial.reminderDays in reminderDayOptions) initial.reminderDays else 7) }
    val maxDay = YearMonth.of(2024, month).lengthOfMonth()
    if (day > maxDay) day = maxDay

    EditorDialog(
        title = if (initial.id.isBlank()) "Neuer Eintrag" else "Bearbeiten",
        onDismiss = onDismiss,
        saveEnabled = name.isNotBlank(),
        onDelete = if (initial.id.isBlank()) null else ({ session.store.delete(Collections.SPECIAL_DAYS, initial.id) }),
        onSave = {
            session.store.save(
                Collections.SPECIAL_DAYS, initial.id,
                initial.copy(name = name.trim(), type = type, day = day, month = month,
                    year = yearText.toIntOrNull() ?: 0, notes = notes.trim(), reminderDays = reminder),
            )
        },
    ) {
        OptionPicker("Art", SpecialDayType.labels, type, { type = it })
        OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text(if (type == SpecialDayType.BIRTHDAY) "Name (z. B. Mama)" else "Bezeichnung (z. B. Hochzeitstag)") })
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OptionPicker("Tag", (1..maxDay).associateWith { "$it." }, day, { day = it })
            OptionPicker("Monat", (1..12).associateWith { monthName(it) }, month, { month = it })
        }
        OutlinedTextField(yearText, { yearText = it.filter(Char::isDigit).take(4) },
            label = { Text("Jahr (optional, für Alter)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        OptionPicker("Erinnerung", reminderDayOptions, reminder, { reminder = it })
        OutlinedTextField(notes, { notes = it }, label = { Text("Notizen / Geschenkideen") }, minLines = 3,
            modifier = Modifier.fillMaxWidth())
        Column { }
    }
}
