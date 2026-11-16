@file:OptIn(ExperimentalMaterial3Api::class)

package de.unseralltag.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.unseralltag.ui.LocalSession
import de.unseralltag.util.formatLong
import de.unseralltag.util.toUtcMillis
import de.unseralltag.util.utcMillisToLocalDate
import java.time.LocalDate
import java.time.LocalTime

/** Beobachtet eine Collection des Haushalts als Compose-State. */
@Composable
fun <T : Any> collection(name: String, clazz: Class<T>): State<List<T>> {
    val store = LocalSession.current.store
    val flow = remember(store.householdId, name) { store.observe(name, clazz) }
    return flow.collectAsState(initial = emptyList())
}

@Composable
inline fun <reified T : Any> collection(name: String): State<List<T>> = collection(name, T::class.java)

@Composable
fun EmptyState(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun ColorDot(color: Color, size: Int = 10) {
    Box(Modifier.size(size.dp).background(color, CircleShape))
}

/** Auswahl "Beide / Person A / Person B". Leerer String = beide. */
@Composable
fun PersonSelector(selected: String, onSelect: (String) -> Unit, allowBoth: Boolean = true, label: String = "Für") {
    val session = LocalSession.current
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (allowBoth) {
                FilterChip(selected == "", { onSelect("") }, { Text("Beide") },
                    leadingIcon = { ColorDot(session.colorOf("")) })
            }
            session.members.forEach { uid ->
                FilterChip(selected == uid, { onSelect(uid) }, { Text(session.nameOf(uid)) },
                    leadingIcon = { ColorDot(session.colorOf(uid)) })
            }
        }
    }
}

/** Einfaches Auswahlfeld für feste Optionen. */
@Composable
fun <K> OptionPicker(label: String, options: Map<K, String>, selected: K, onSelect: (K) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { open = true }) {
                Text(options[selected] ?: "")
                Icon(Icons.Default.ArrowDropDown, null)
            }
            DropdownMenu(open, { open = false }) {
                options.forEach { (k, v) ->
                    DropdownMenuItem(text = { Text(v) }, onClick = { onSelect(k); open = false })
                }
            }
        }
    }
}

@Composable
fun DateField(label: String, date: LocalDate?, onChange: (LocalDate?) -> Unit, clearable: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { open = true }) { Text(date?.formatLong() ?: "Kein Datum") }
            if (clearable && date != null) {
                IconButton(onClick = { onChange(null) }) { Icon(Icons.Default.Close, "Datum entfernen") }
            }
        }
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (date ?: LocalDate.now()).toUtcMillis())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(utcMillisToLocalDate(it)) }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Abbrechen") } },
        ) { DatePicker(state) }
    }
}

@Composable
fun TimeField(label: String, time: LocalTime, onChange: (LocalTime) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { open = true }) { Text(String.format("%02d:%02d", time.hour, time.minute)) }
    }
    if (open) {
        val state = rememberTimePickerState(time.hour, time.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = { onChange(LocalTime.of(state.hour, state.minute)); open = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Abbrechen") } },
            text = { TimePicker(state) },
        )
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String = "Löschen", onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

/**
 * Vollbild-Editor mit "Speichern" und optional "Löschen".
 */
@Composable
fun EditorDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean = true,
    onDelete: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") }
                    },
                    actions = {
                        if (onDelete != null) {
                            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Löschen") }
                        }
                        TextButton(enabled = saveEnabled, onClick = { onSave(); onDismiss() }) { Text("Speichern") }
                    },
                )
            },
        ) { pad ->
            Column(
                Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
        }
    }
    if (confirmDelete && onDelete != null) {
        ConfirmDialog("Wirklich löschen?", "Das kann nicht rückgängig gemacht werden.",
            onConfirm = { onDelete(); onDismiss() }, onDismiss = { confirmDelete = false })
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 16.dp, bottom = 4.dp, start = 16.dp, end = 16.dp),
    )
}

@Composable
fun VSpace(h: Int) = Spacer(Modifier.height(h.dp))
