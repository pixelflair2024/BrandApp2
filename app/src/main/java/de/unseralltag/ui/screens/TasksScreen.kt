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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import de.unseralltag.data.Collections
import de.unseralltag.data.HouseholdStore
import de.unseralltag.data.Recurrence
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
import de.unseralltag.ui.common.SectionHeader
import de.unseralltag.ui.common.collection
import de.unseralltag.util.relativeDay
import de.unseralltag.util.toLocalDate
import de.unseralltag.util.today
import java.time.LocalDate

/** Abhaken: wiederkehrende Aufgaben springen auf den nächsten Termin. */
fun toggleTask(store: HouseholdStore, t: TaskItem, done: Boolean) {
    if (done && t.recurrence != Recurrence.NONE) {
        val base = maxOf(t.dueDay?.toLocalDate() ?: today(), today())
        val next = when (t.recurrence) {
            Recurrence.DAILY -> (t.dueDay?.toLocalDate() ?: today()).plusDays(1).let { if (it <= today()) today().plusDays(1) else it }
            Recurrence.WEEKLY -> (t.dueDay?.toLocalDate() ?: base).plusWeeks(1)
            Recurrence.MONTHLY -> (t.dueDay?.toLocalDate() ?: base).plusMonths(1)
            Recurrence.YEARLY -> (t.dueDay?.toLocalDate() ?: base).plusYears(1)
            else -> base
        }
        store.update(Collections.TASKS, t.id, mapOf("dueDay" to next.toEpochDay(), "done" to false, "doneAt" to System.currentTimeMillis()))
    } else {
        store.update(Collections.TASKS, t.id, mapOf("done" to done, "doneAt" to if (done) System.currentTimeMillis() else 0L))
    }
}

@Composable
fun TasksScreen(nav: Nav) {
    val session = LocalSession.current
    val tasks by collection<TaskItem>(Collections.TASKS)
    var filter by rememberSaveable { mutableStateOf("all") }
    var editing by remember { mutableStateOf<TaskItem?>(null) }
    val today = today()

    val filtered = tasks.filter {
        when (filter) {
            "mine" -> it.assignedTo == session.myUid || it.assignedTo.isBlank()
            else -> true
        }
    }
    val open = filtered.filter { !it.done }
        .sortedWith(compareBy<TaskItem>({ it.dueDay == null }, { it.dueDay ?: Long.MAX_VALUE }, { -it.createdAt }))
    val done = filtered.filter { it.done }.sortedByDescending { it.doneAt }.take(30)

    ScreenScaffold("Aufgaben", onAdd = { editing = TaskItem() }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad)) {
            item {
                LazyRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(filter == "all", { filter = "all" }, { Text("Alle") }) }
                    item { FilterChip(filter == "mine", { filter = "mine" }, { Text("Meine") }) }
                    item { FilterChip(filter == "done", { filter = "done" }, { Text("Erledigt") }) }
                }
            }
            val shown = if (filter == "done") tasks.filter { it.done }.sortedByDescending { it.doneAt } else open
            if (shown.isEmpty()) item { EmptyState(Icons.Default.CheckCircle, "Keine offenen Aufgaben 🎉") }
            items(shown, key = { it.id }) { t -> TaskRow(t, today) { editing = t } }
            if (filter != "done" && done.isNotEmpty()) {
                item { SectionHeader("Kürzlich erledigt") }
                items(done.take(5), key = { "d" + it.id }) { t -> TaskRow(t, today) { editing = t } }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    editing?.let { TaskEditor(it) { editing = null } }
}

@Composable
private fun TaskRow(t: TaskItem, today: LocalDate, onClick: () -> Unit) {
    val session = LocalSession.current
    val due = t.dueDay?.toLocalDate()
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Checkbox(t.done, { toggleTask(session.store, t, it) }) },
        headlineContent = {
            Text(t.title, textDecoration = if (t.done) TextDecoration.LineThrough else null)
        },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorDot(session.colorOf(t.assignedTo), 8)
                Spacer(Modifier.width(6.dp))
                Text(session.nameOf(t.assignedTo))
                if (due != null) {
                    Text(" · ")
                    Text(relativeDay(due, today),
                        color = if (!t.done && due < today) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (t.recurrence != Recurrence.NONE) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.Repeat, "Wiederkehrend", Modifier.padding(0.dp))
                }
            }
        },
    )
}

@Composable
private fun TaskEditor(initial: TaskItem, onDismiss: () -> Unit) {
    val session = LocalSession.current
    var title by remember { mutableStateOf(initial.title) }
    var notes by remember { mutableStateOf(initial.notes) }
    var due by remember { mutableStateOf(initial.dueDay?.toLocalDate()) }
    var assigned by remember { mutableStateOf(initial.assignedTo) }
    var recurrence by remember { mutableStateOf(if (initial.recurrence in Recurrence.labels) initial.recurrence else Recurrence.NONE) }
    EditorDialog(
        title = if (initial.id.isBlank()) "Neue Aufgabe" else "Aufgabe bearbeiten",
        onDismiss = onDismiss,
        saveEnabled = title.isNotBlank(),
        onDelete = if (initial.id.isBlank()) null else ({ session.store.delete(Collections.TASKS, initial.id) }),
        onSave = {
            session.store.save(
                Collections.TASKS, initial.id,
                initial.copy(
                    title = title.trim(), notes = notes.trim(), assignedTo = assigned, recurrence = recurrence,
                    dueDay = (due ?: if (recurrence != Recurrence.NONE) today() else null)?.toEpochDay(),
                    createdAt = if (initial.createdAt == 0L) System.currentTimeMillis() else initial.createdAt,
                ),
            )
        },
    ) {
        OutlinedTextField(title, { title = it }, label = { Text("Was ist zu tun?") }, modifier = Modifier.fillMaxWidth())
        PersonSelector(assigned, { assigned = it }, label = "Wer?")
        DateField("Fällig am", due, { due = it }, clearable = true)
        OptionPicker("Wiederholen", Recurrence.labels, recurrence, { recurrence = it })
        OutlinedTextField(notes, { notes = it }, label = { Text("Notizen") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        Column { }
    }
}
