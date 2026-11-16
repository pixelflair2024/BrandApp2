package de.unseralltag.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.unseralltag.data.Collections
import de.unseralltag.data.Note
import de.unseralltag.ui.LocalSession
import de.unseralltag.ui.Nav
import de.unseralltag.ui.ScreenScaffold
import de.unseralltag.ui.common.EditorDialog
import de.unseralltag.ui.common.EmptyState
import de.unseralltag.ui.common.collection
import de.unseralltag.util.formatShort
import de.unseralltag.util.millisToDate

@Composable
fun NotesScreen(nav: Nav) {
    val session = LocalSession.current
    val notes by collection<Note>(Collections.NOTES)
    var editing by remember { mutableStateOf<Note?>(null) }
    var query by remember { mutableStateOf("") }
    val shown = notes
        .filter { query.isBlank() || it.title.contains(query, true) || it.content.contains(query, true) }
        .sortedWith(compareBy<Note>({ !it.pinned }, { -it.updatedAt }))

    ScreenScaffold("Notizen", onBack = nav::back, onAdd = { editing = Note() }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            OutlinedTextField(query, { query = it }, placeholder = { Text("Suchen …") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            if (shown.isEmpty()) EmptyState(Icons.AutoMirrored.Filled.StickyNote2,
                "Hier ist Platz für WLAN-Passwort, Rezepte, Ideen …")
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp,
            ) {
                items(shown, key = { it.id }) { n ->
                    Card(Modifier.fillMaxWidth().clickable { editing = n }) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text(n.title.ifBlank { "Ohne Titel" }, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                if (n.pinned) Icon(Icons.Default.PushPin, "Angeheftet", tint = MaterialTheme.colorScheme.primary)
                            }
                            Text(n.content, maxLines = 8, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                            Text("${session.nameOf(n.updatedBy)} · ${n.updatedAt.millisToDate().formatShort()}",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
        }
    }
    editing?.let { NoteEditor(it) { editing = null } }
}

@Composable
private fun NoteEditor(initial: Note, onDismiss: () -> Unit) {
    val session = LocalSession.current
    var title by remember { mutableStateOf(initial.title) }
    var content by remember { mutableStateOf(initial.content) }
    var pinned by remember { mutableStateOf(initial.pinned) }
    EditorDialog(
        title = if (initial.id.isBlank()) "Neue Notiz" else "Notiz",
        onDismiss = onDismiss,
        saveEnabled = title.isNotBlank() || content.isNotBlank(),
        onDelete = if (initial.id.isBlank()) null else ({ session.store.delete(Collections.NOTES, initial.id) }),
        onSave = {
            session.store.save(Collections.NOTES, initial.id,
                initial.copy(title = title.trim(), content = content.trimEnd(), pinned = pinned,
                    updatedAt = System.currentTimeMillis(), updatedBy = session.myUid))
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(title, { title = it }, label = { Text("Titel") }, singleLine = true, modifier = Modifier.weight(1f))
            IconToggleButton(pinned, { pinned = it }) {
                Icon(if (pinned) Icons.Default.PushPin else Icons.Outlined.PushPin, "Anheften")
            }
        }
        OutlinedTextField(content, { content = it }, label = { Text("Inhalt") }, minLines = 12, modifier = Modifier.fillMaxWidth())
    }
}
