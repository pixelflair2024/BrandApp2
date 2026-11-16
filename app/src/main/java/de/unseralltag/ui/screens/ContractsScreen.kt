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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import de.unseralltag.data.Contract
import de.unseralltag.data.CostInterval
import de.unseralltag.data.NoticeUnit
import de.unseralltag.ui.LocalSession
import de.unseralltag.ui.Nav
import de.unseralltag.ui.ScreenScaffold
import de.unseralltag.ui.common.DateField
import de.unseralltag.ui.common.EditorDialog
import de.unseralltag.ui.common.EmptyState
import de.unseralltag.ui.common.OptionPicker
import de.unseralltag.ui.common.collection
import de.unseralltag.util.cancelDeadline
import de.unseralltag.util.daysUntil
import de.unseralltag.util.formatEuro
import de.unseralltag.util.formatNumeric
import de.unseralltag.util.monthlyCost
import de.unseralltag.util.relativeDay
import de.unseralltag.util.toLocalDate
import de.unseralltag.util.today

@Composable
fun ContractsScreen(nav: Nav) {
    val contracts by collection<Contract>(Collections.CONTRACTS)
    var editing by remember { mutableStateOf<Contract?>(null) }
    val today = today()
    val sorted = contracts.sortedWith(compareBy({ it.cancelDeadline() == null }, { it.cancelDeadline() }, { it.name }))
    val monthly = contracts.sumOf { it.monthlyCost() }

    ScreenScaffold("Verträge & Fristen", onBack = nav::back, onAdd = { editing = Contract() }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (contracts.isNotEmpty()) item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Laufende Kosten", style = MaterialTheme.typography.labelLarge)
                        Text("${formatEuro(monthly)} / Monat", style = MaterialTheme.typography.headlineSmall)
                        Text("${formatEuro(monthly * 12)} / Jahr", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (contracts.isEmpty()) item {
                EmptyState(Icons.Default.Description,
                    "Behaltet Handy-, Strom-, Versicherungs- und Abo-Verträge im Blick – inklusive Kündigungsfrist.")
            }
            items(sorted, key = { it.id }) { c ->
                val deadline = c.cancelDeadline()
                val left = deadline?.let { daysUntil(it, today) }
                ListItem(
                    modifier = Modifier.clickable { editing = c },
                    headlineContent = { Text(c.name) },
                    supportingContent = {
                        Column {
                            val parts = listOfNotNull(
                                c.provider.ifBlank { null },
                                if (c.cost > 0) "${formatEuro(c.cost)} ${CostInterval.labels[c.costInterval]}" else null,
                            )
                            if (parts.isNotEmpty()) Text(parts.joinToString(" · "))
                            c.endDay?.let { Text("Läuft bis ${it.toLocalDate().formatNumeric()}") }
                        }
                    },
                    trailingContent = {
                        if (deadline != null) Column(horizontalAlignment = Alignment.End) {
                            Text("Kündigen bis", style = MaterialTheme.typography.labelSmall)
                            Text(deadline.formatNumeric(), style = MaterialTheme.typography.labelLarge,
                                color = when {
                                    left!! < 0 -> MaterialTheme.colorScheme.outline
                                    left <= 30 -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface
                                })
                            if (left in 0..60) Text(relativeDay(deadline, today), style = MaterialTheme.typography.labelSmall)
                        }
                    },
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    editing?.let { ContractEditor(it) { editing = null } }
}

private val reminderDayOptions = linkedMapOf(7 to "1 Woche vorher", 14 to "2 Wochen vorher", 30 to "1 Monat vorher", 60 to "2 Monate vorher")

@Composable
private fun ContractEditor(initial: Contract, onDismiss: () -> Unit) {
    val session = LocalSession.current
    var name by remember { mutableStateOf(initial.name) }
    var provider by remember { mutableStateOf(initial.provider) }
    var cost by remember { mutableStateOf(if (initial.cost > 0) initial.cost.toString().replace('.', ',') else "") }
    var interval by remember { mutableStateOf(initial.costInterval) }
    var end by remember { mutableStateOf(initial.endDay?.toLocalDate()) }
    var notice by remember { mutableStateOf(if (initial.noticeValue > 0) initial.noticeValue.toString() else "") }
    var unit by remember { mutableStateOf(initial.noticeUnit) }
    var reminder by remember { mutableStateOf(if (initial.reminderDays in reminderDayOptions) initial.reminderDays else 14) }
    var notes by remember { mutableStateOf(initial.notes) }

    val preview = initial.copy(endDay = end?.toEpochDay(), noticeValue = notice.toIntOrNull() ?: 0, noticeUnit = unit).cancelDeadline()

    EditorDialog(
        title = if (initial.id.isBlank()) "Neuer Vertrag" else "Vertrag bearbeiten",
        onDismiss = onDismiss,
        saveEnabled = name.isNotBlank(),
        onDelete = if (initial.id.isBlank()) null else ({ session.store.delete(Collections.CONTRACTS, initial.id) }),
        onSave = {
            session.store.save(Collections.CONTRACTS, initial.id,
                initial.copy(
                    name = name.trim(), provider = provider.trim(),
                    cost = cost.replace(',', '.').toDoubleOrNull() ?: 0.0, costInterval = interval,
                    endDay = end?.toEpochDay(), noticeValue = notice.toIntOrNull() ?: 0, noticeUnit = unit,
                    reminderDays = reminder, notes = notes.trim(),
                ))
        },
    ) {
        OutlinedTextField(name, { name = it }, label = { Text("Vertrag (z. B. Handyvertrag)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(provider, { provider = it }, label = { Text("Anbieter") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(cost, { cost = it }, label = { Text("Kosten in €") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            OptionPicker("", CostInterval.labels, interval, { interval = it })
        }
        DateField("Vertragsende / nächste Verlängerung", end, { end = it }, clearable = true)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(notice, { notice = it.filter(Char::isDigit).take(3) }, label = { Text("Kündigungsfrist") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            OptionPicker("", NoticeUnit.labels, unit, { unit = it })
        }
        if (preview != null) {
            Text("→ Spätestens kündigen am ${preview.formatNumeric()}", color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall)
        }
        OptionPicker("Erinnerung", reminderDayOptions, reminder, { reminder = it })
        OutlinedTextField(notes, { notes = it }, label = { Text("Notizen (Kundennr., Kontakt …)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
    }
}
