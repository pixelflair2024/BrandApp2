package de.unseralltag.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.widget.Toast
import de.unseralltag.data.Collections
import de.unseralltag.data.Meal
import de.unseralltag.data.ShoppingList
import de.unseralltag.ui.LocalSession
import de.unseralltag.ui.Nav
import de.unseralltag.ui.ScreenScaffold
import de.unseralltag.ui.common.DateField
import de.unseralltag.ui.common.EditorDialog
import de.unseralltag.ui.common.OptionPicker
import de.unseralltag.ui.common.collection
import de.unseralltag.util.GERMAN
import de.unseralltag.util.formatShort
import de.unseralltag.util.toLocalDate
import de.unseralltag.util.today
import java.time.LocalDate
import java.time.format.TextStyle

@Composable
fun MealPlanScreen(nav: Nav) {
    val meals by collection<Meal>(Collections.MEALS)
    var weekStartStr by rememberSaveable {
        mutableStateOf(today().let { it.minusDays((it.dayOfWeek.value - 1).toLong()) }.toString())
    }
    val weekStart = LocalDate.parse(weekStartStr)
    var editing by remember { mutableStateOf<Meal?>(null) }
    val days = (0L until 7L).map { weekStart.plusDays(it) }
    // Bisher gekochte Gerichte als Vorlage
    val history = remember(meals) { meals.sortedByDescending { it.day }.distinctBy { it.title.lowercase() } }

    ScreenScaffold("Essensplan", onBack = nav::back, onAdd = { editing = Meal(day = today().toEpochDay()) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { weekStartStr = weekStart.minusWeeks(1).toString() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Vorherige Woche")
                    }
                    Text("${weekStart.formatShort()} – ${weekStart.plusDays(6).formatShort()}", Modifier.weight(1f),
                        textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { weekStartStr = weekStart.plusWeeks(1).toString() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Nächste Woche")
                    }
                }
            }
            items(days, key = { it.toEpochDay() }) { day ->
                val dayMeals = meals.filter { it.day == day.toEpochDay() }
                val isToday = day == today()
                Card(
                    Modifier.fillMaxWidth(),
                    colors = if (isToday) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    else CardDefaults.cardColors(),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(day.dayOfWeek.getDisplayName(TextStyle.FULL, GERMAN) + ", " + day.dayOfMonth + ".",
                                Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            IconButton(onClick = { editing = Meal(day = day.toEpochDay()) }) { Icon(Icons.Default.Add, "Gericht hinzufügen") }
                        }
                        if (dayMeals.isEmpty()) Text("–", color = MaterialTheme.colorScheme.outline)
                        dayMeals.forEach { m ->
                            Text(m.title, Modifier.fillMaxWidth().clickable { editing = m }.padding(vertical = 4.dp),
                                style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    editing?.let { MealEditor(it, history) { editing = null } }
}

@Composable
private fun MealEditor(initial: Meal, history: List<Meal>, onDismiss: () -> Unit) {
    val session = LocalSession.current
    val context = LocalContext.current
    val lists by collection<ShoppingList>(Collections.SHOPPING_LISTS)
    var title by remember { mutableStateOf(initial.title) }
    var day by remember { mutableStateOf(initial.day.toLocalDate()) }
    var ingredients by remember { mutableStateOf(initial.ingredients.joinToString("\n")) }
    var notes by remember { mutableStateOf(initial.notes) }
    var targetList by remember { mutableStateOf("") }
    val sortedLists = lists.sortedBy { it.order }
    val target = sortedLists.firstOrNull { it.id == targetList } ?: sortedLists.firstOrNull()
    val suggestions = if (initial.id.isBlank() && title.length >= 1)
        history.filter { it.title.contains(title, true) && !it.title.equals(title, true) }.take(4) else emptyList()

    EditorDialog(
        title = if (initial.id.isBlank()) "Gericht planen" else "Gericht",
        onDismiss = onDismiss,
        saveEnabled = title.isNotBlank(),
        onDelete = if (initial.id.isBlank()) null else ({ session.store.delete(Collections.MEALS, initial.id) }),
        onSave = {
            session.store.save(Collections.MEALS, initial.id,
                initial.copy(title = title.trim(), day = day.toEpochDay(), notes = notes.trim(),
                    ingredients = ingredients.lines().map { it.trim() }.filter { it.isNotBlank() }))
        },
    ) {
        OutlinedTextField(title, { title = it }, label = { Text("Gericht") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (suggestions.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { s ->
                    AssistChip({
                        title = s.title; ingredients = s.ingredients.joinToString("\n"); notes = s.notes
                    }, { Text(s.title) })
                }
            }
        }
        DateField("Tag", day, { it?.let { d -> day = d } })
        OutlinedTextField(ingredients, { ingredients = it }, label = { Text("Zutaten (eine pro Zeile)") }, minLines = 4,
            modifier = Modifier.fillMaxWidth())
        if (sortedLists.isNotEmpty()) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionPicker("Auf Liste", sortedLists.associate { it.id to it.name }, target?.id ?: "", { targetList = it })
                OutlinedButton(
                    enabled = ingredients.isNotBlank() && target != null,
                    onClick = {
                        val lines = ingredients.lines().map { it.trim() }.filter { it.isNotBlank() }
                        lines.forEach { addShoppingItem(session.store, target!!.id, it, session.myUid) }
                        Toast.makeText(context, "${lines.size} Zutaten auf „${target!!.name}“ gesetzt", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Icon(Icons.Default.AddShoppingCart, null); Text(" Einkaufen")
                }
            }
        }
        OutlinedTextField(notes, { notes = it }, label = { Text("Notizen / Rezept-Link") }, minLines = 3, modifier = Modifier.fillMaxWidth())
    }
}
