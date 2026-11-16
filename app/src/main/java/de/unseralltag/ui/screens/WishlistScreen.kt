package de.unseralltag.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import de.unseralltag.data.Collections
import de.unseralltag.data.WishItem
import de.unseralltag.ui.LocalSession
import de.unseralltag.ui.Nav
import de.unseralltag.ui.ScreenScaffold
import de.unseralltag.ui.common.EditorDialog
import de.unseralltag.ui.common.EmptyState
import de.unseralltag.ui.common.collection

@Composable
fun WishlistScreen(nav: Nav) {
    val session = LocalSession.current
    val all by collection<WishItem>(Collections.WISHES)
    var tabUid by rememberSaveable { mutableStateOf(session.myUid) }
    var editing by remember { mutableStateOf<WishItem?>(null) }
    val members = listOf(session.myUid) + session.members.filter { it != session.myUid }
    val isMine = tabUid == session.myUid

    // Geschenkideen, die jemand anderes auf meine Liste gesetzt hat, sind für mich unsichtbar.
    val visible = all.filter { it.ownerUid == tabUid }
        .filter { !(it.ownerUid == session.myUid && it.createdBy != session.myUid && it.createdBy.isNotBlank()) }
        .sortedWith(compareBy({ !isMine && it.purchasedBy.isNotBlank() }, { -it.createdAt }))

    ScreenScaffold("Wunschlisten", onBack = nav::back, onAdd = { editing = WishItem(ownerUid = tabUid) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            TabRow(members.indexOf(tabUid).coerceAtLeast(0)) {
                members.forEach { uid ->
                    Tab(tabUid == uid, { tabUid = uid },
                        text = { Text(if (uid == session.myUid) "Meine Wünsche" else session.nameOf(uid)) })
                }
            }
            if (!isMine) {
                Text(
                    "Was du hier hinzufügst, ist eine geheime Geschenkidee – ${session.nameOf(tabUid)} sieht sie nicht. " +
                        "Markierungen als „besorgt“ bleiben ebenfalls geheim.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (visible.isEmpty()) item {
                    EmptyState(Icons.Default.CardGiftcard,
                        if (isMine) "Noch keine Wünsche. Was wünschst du dir?" else "Noch keine Wünsche oder Ideen.")
                }
                items(visible, key = { it.id }) { w -> WishCard(w, isMine) { editing = w } }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
    editing?.let { WishEditor(it) { editing = null } }
}

@Composable
private fun WishCard(w: WishItem, isMine: Boolean, onEdit: () -> Unit) {
    val session = LocalSession.current
    val context = LocalContext.current
    val purchased = !isMine && w.purchasedBy.isNotBlank()
    val secret = w.createdBy.isNotBlank() && w.createdBy != w.ownerUid
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onEdit),
        colors = if (purchased) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        else CardDefaults.cardColors(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (secret) Icon(Icons.Default.Lock, "Geheime Idee", Modifier.padding(end = 6.dp), tint = MaterialTheme.colorScheme.tertiary)
                Text(w.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (purchased) TextDecoration.LineThrough else null)
                if (w.price.isNotBlank()) Text(w.price, style = MaterialTheme.typography.labelLarge)
                if (w.url.isNotBlank()) IconButton(onClick = {
                    val url = if (w.url.startsWith("http")) w.url else "https://${w.url}"
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                }) { Icon(Icons.AutoMirrored.Filled.OpenInNew, "Link öffnen") }
            }
            if (w.notes.isNotBlank()) Text(w.notes, style = MaterialTheme.typography.bodyMedium)
            if (!isMine) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (w.purchasedBy.isBlank()) {
                        AssistChip({ session.store.update(Collections.WISHES, w.id, mapOf("purchasedBy" to session.myUid)) },
                            { Text("Ich besorge das") }, leadingIcon = { Icon(Icons.Default.CardGiftcard, null) })
                    } else {
                        FilterChip(true, {
                            if (w.purchasedBy == session.myUid) session.store.update(Collections.WISHES, w.id, mapOf("purchasedBy" to ""))
                        }, { Text("Besorgt von ${session.nameOf(w.purchasedBy)}") })
                    }
                }
            }
        }
    }
}

@Composable
private fun WishEditor(initial: WishItem, onDismiss: () -> Unit) {
    val session = LocalSession.current
    var title by remember { mutableStateOf(initial.title) }
    var url by remember { mutableStateOf(initial.url) }
    var price by remember { mutableStateOf(initial.price) }
    var notes by remember { mutableStateOf(initial.notes) }
    EditorDialog(
        title = if (initial.id.isBlank()) "Neuer Wunsch" else "Wunsch bearbeiten",
        onDismiss = onDismiss,
        saveEnabled = title.isNotBlank(),
        onDelete = if (initial.id.isBlank()) null else ({ session.store.delete(Collections.WISHES, initial.id) }),
        onSave = {
            session.store.save(
                Collections.WISHES, initial.id,
                initial.copy(
                    title = title.trim(), url = url.trim(), price = price.trim(), notes = notes.trim(),
                    createdBy = initial.createdBy.ifBlank { session.myUid },
                    createdAt = if (initial.createdAt == 0L) System.currentTimeMillis() else initial.createdAt,
                ),
            )
        },
    ) {
        Text("Für: ${session.nameOf(initial.ownerUid)}", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(title, { title = it }, label = { Text("Was?") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(url, { url = it }, label = { Text("Link (optional)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(price, { price = it }, label = { Text("Preis (optional)") }, singleLine = true,
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(notes, { notes = it }, label = { Text("Notizen (Größe, Farbe, …)") }, minLines = 3,
            modifier = Modifier.fillMaxWidth())
    }
}
