package de.unseralltag.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import de.unseralltag.data.Collections
import de.unseralltag.data.HouseholdStore
import de.unseralltag.data.ShoppingItem
import de.unseralltag.data.ShoppingList
import de.unseralltag.ui.LocalSession
import de.unseralltag.ui.Nav
import de.unseralltag.ui.ScreenScaffold
import de.unseralltag.ui.common.ConfirmDialog
import de.unseralltag.ui.common.EmptyState
import de.unseralltag.ui.common.collection

/** "2 Milch" / "Milch 2" / "500g Mehl" → Menge + Name */
fun parseItem(text: String): Pair<String, String> {
    val t = text.trim()
    val m = Regex("""^(\d+[.,]?\d*\s*(x|g|kg|l|ml|st|stk|pck|packung|dose|dosen|flaschen?)?)\s+(.+)$""", RegexOption.IGNORE_CASE).find(t)
    return if (m != null) m.groupValues[1].trim() to m.groupValues[3].trim() else "" to t
}

fun addShoppingItem(store: HouseholdStore, listId: String, text: String, uid: String) {
    val (qty, name) = parseItem(text)
    if (name.isBlank()) return
    store.save(
        Collections.SHOPPING_ITEMS, "",
        ShoppingItem(listId = listId, name = name, quantity = qty, createdAt = System.currentTimeMillis(), createdBy = uid),
    )
}

@Composable
fun ShoppingScreen(nav: Nav) {
    val session = LocalSession.current
    val store = session.store
    val lists by collection<ShoppingList>(Collections.SHOPPING_LISTS)
    val items by collection<ShoppingItem>(Collections.SHOPPING_ITEMS)
    val sortedLists = lists.sortedWith(compareBy({ it.order }, { it.name }))
    var selectedId by rememberSaveable { mutableStateOf("") }
    val selected = sortedLists.firstOrNull { it.id == selectedId } ?: sortedLists.firstOrNull()
    var input by rememberSaveable { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var listDialog by remember { mutableStateOf<ShoppingList?>(null) }
    var confirmDeleteList by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<ShoppingItem?>(null) }

    val listItems = items.filter { it.listId == selected?.id }
    val open = listItems.filter { !it.checked }.sortedBy { it.createdAt }
    val done = listItems.filter { it.checked }.sortedBy { it.name.lowercase() }
    val suggestions = if (input.length >= 2) {
        val q = parseItem(input).second.lowercase()
        items.map { it.name }.distinctBy { it.lowercase() }
            .filter { it.lowercase().startsWith(q) && open.none { o -> o.name.equals(it, true) } && !it.equals(q, true) }
            .take(5)
    } else emptyList()

    fun submit(text: String) {
        val l = selected ?: return
        addShoppingItem(store, l.id, text, session.myUid)
        input = ""
    }

    ScreenScaffold(
        title = "Einkauf",
        actions = {
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Menü") }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem({ Text("Neue Liste") }, { menu = false; listDialog = ShoppingList(order = System.currentTimeMillis()) })
                if (selected != null) {
                    DropdownMenuItem({ Text("Liste umbenennen") }, { menu = false; listDialog = selected })
                    DropdownMenuItem({ Text("Erledigte entfernen") }, {
                        menu = false; done.forEach { store.delete(Collections.SHOPPING_ITEMS, it.id) }
                    })
                    DropdownMenuItem({ Text("Liste löschen") }, { menu = false; confirmDeleteList = true })
                }
            }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).imePadding()) {
            LazyRow(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sortedLists, key = { it.id }) { l ->
                    val count = items.count { it.listId == l.id && !it.checked }
                    FilterChip(l.id == selected?.id, { selectedId = l.id },
                        { Text(if (count > 0) "${l.name} ($count)" else l.name) })
                }
                item {
                    AssistChip({ listDialog = ShoppingList(order = System.currentTimeMillis()) }, { Text("Liste") },
                        leadingIcon = { Icon(Icons.Default.Add, null) })
                }
            }
            LazyColumn(Modifier.weight(1f)) {
                if (selected == null) {
                    item { EmptyState(Icons.Default.ShoppingCart, "Lege zuerst eine Einkaufsliste an.") }
                } else if (listItems.isEmpty()) {
                    item { EmptyState(Icons.Default.ShoppingCart, "Die Liste ist leer.\nTippe unten ein, was ihr braucht.") }
                }
                items(open, key = { it.id }) { item -> ItemRow(item, store) { editItem = item } }
                if (done.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Erledigt (${done.size})", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.outline)
                            TextButton(onClick = { done.forEach { store.delete(Collections.SHOPPING_ITEMS, it.id) } }) { Text("Entfernen") }
                        }
                    }
                    items(done, key = { it.id }) { item -> ItemRow(item, store) { editItem = item } }
                }
            }
            if (selected != null) {
                Surface(tonalElevation = 3.dp) {
                    Column(Modifier.padding(8.dp)) {
                        if (suggestions.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(suggestions) { s -> AssistChip({ submit(s) }, { Text(s) }) }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                input, { input = it }, Modifier.weight(1f),
                                placeholder = { Text("z. B. 2 Milch") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { submit(input) }),
                            )
                            IconButton(enabled = input.isNotBlank(), onClick = { submit(input) }) {
                                Icon(Icons.AutoMirrored.Filled.Send, "Hinzufügen")
                            }
                        }
                    }
                }
            }
        }
    }

    listDialog?.let { l ->
        var name by remember(l) { mutableStateOf(l.name) }
        AlertDialog(
            onDismissRequest = { listDialog = null },
            title = { Text(if (l.id.isBlank()) "Neue Liste" else "Liste umbenennen") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("Name (z. B. Drogerie)") }, singleLine = true) },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    if (l.id.isBlank()) {
                        val ref = store.col(Collections.SHOPPING_LISTS).document()
                        ref.set(l.copy(name = name.trim()))
                        selectedId = ref.id
                    } else store.update(Collections.SHOPPING_LISTS, l.id, mapOf("name" to name.trim()))
                    listDialog = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { listDialog = null }) { Text("Abbrechen") } },
        )
    }
    if (confirmDeleteList && selected != null) {
        ConfirmDialog("Liste „${selected.name}“ löschen?", "Alle Artikel darin werden ebenfalls gelöscht.",
            onConfirm = {
                listItems.forEach { store.delete(Collections.SHOPPING_ITEMS, it.id) }
                store.delete(Collections.SHOPPING_LISTS, selected.id)
                selectedId = ""
            },
            onDismiss = { confirmDeleteList = false })
    }
    editItem?.let { it ->
        var name by remember(it) { mutableStateOf(it.name) }
        var qty by remember(it) { mutableStateOf(it.quantity) }
        AlertDialog(
            onDismissRequest = { editItem = null },
            title = { Text("Artikel bearbeiten") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Artikel") }, singleLine = true)
                    OutlinedTextField(qty, { qty = it }, label = { Text("Menge") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    store.update(Collections.SHOPPING_ITEMS, it.id, mapOf("name" to name.trim(), "quantity" to qty.trim()))
                    editItem = null
                }) { Text("Speichern") }
            },
            dismissButton = {
                TextButton(onClick = { store.delete(Collections.SHOPPING_ITEMS, it.id); editItem = null }) { Text("Löschen") }
            },
        )
    }
}

@Composable
private fun ItemRow(item: ShoppingItem, store: HouseholdStore, onEdit: () -> Unit) {
    Column {
        ListItem(
            modifier = Modifier.clickable { store.update(Collections.SHOPPING_ITEMS, item.id, mapOf("checked" to !item.checked)) },
            leadingContent = {
                Checkbox(item.checked, { store.update(Collections.SHOPPING_ITEMS, item.id, mapOf("checked" to it)) })
            },
            headlineContent = {
                Text(
                    item.name,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                    color = if (item.checked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                )
            },
            supportingContent = if (item.quantity.isNotBlank()) ({ Text(item.quantity) }) else null,
            trailingContent = {
                IconButton(onClick = onEdit) { Icon(Icons.Default.MoreVert, "Bearbeiten") }
            },
        )
        HorizontalDivider()
    }
}

