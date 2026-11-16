package de.unseralltag.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import de.unseralltag.BuildConfig
import de.unseralltag.data.AccountRepository
import de.unseralltag.reminders.ReminderWorker
import de.unseralltag.ui.LocalSession
import de.unseralltag.ui.Nav
import de.unseralltag.ui.ScreenScaffold
import de.unseralltag.ui.common.ColorDot
import de.unseralltag.ui.common.ConfirmDialog
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(nav: Nav) {
    val session = LocalSession.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var myName by remember { mutableStateOf(session.myName) }
    var hhName by remember { mutableStateOf(session.household.name) }
    var confirmLogout by remember { mutableStateOf(false) }

    ScreenScaffold("Einstellungen", onBack = nav::back) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Haushalt", style = MaterialTheme.typography.titleMedium)
                    session.members.forEach { uid ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ColorDot(session.colorOf(uid), 12)
                            Text(session.nameOf(uid) + if (uid == session.myUid) " (du)" else "")
                        }
                    }
                    if (session.members.size < 2) {
                        Text("Lade deinen Partner ein: App installieren, mit Google anmelden, „Beitreten“ wählen und diesen Code eingeben:",
                            style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Einladungscode:", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(session.household.inviteCode, style = MaterialTheme.typography.headlineMedium,
                            fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Tritt unserem Haushalt in UnserAlltag bei! Code: ${session.household.inviteCode}")
                            }
                            context.startActivity(Intent.createChooser(send, "Code teilen"))
                        }) { Icon(Icons.Default.Share, null); Text(" Teilen") }
                    }
                }
            }
            OutlinedTextField(hhName, { hhName = it }, label = { Text("Name des Haushalts") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(myName, { myName = it }, label = { Text("Dein Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(
                enabled = myName.isNotBlank() && hhName.isNotBlank() &&
                    (myName != session.myName || hhName != session.household.name),
                onClick = {
                    if (hhName != session.household.name) session.store.renameHousehold(hhName.trim())
                    if (myName != session.myName) scope.launch {
                        runCatching { AccountRepository.updateMyName(myName.trim(), session.household.id) }
                    }
                },
            ) { Text("Speichern") }
            HorizontalDivider()
            Text("Erinnerungen", style = MaterialTheme.typography.titleMedium)
            Text(
                "Die App prüft etwa alle 15 Minuten, ob Termine anstehen, und meldet sich morgens ab 8 Uhr " +
                    "mit Geburtstagen, fälligen Aufgaben und Kündigungsfristen.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = { ReminderWorker.runNow(context) }) { Text("Jetzt prüfen") }
            HorizontalDivider()
            Text("Angemeldet als ${runCatching { AccountRepository.currentUser?.email }.getOrNull() ?: "?"}", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { confirmLogout = true }) { Text("Abmelden") }
            Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline)
        }
    }
    if (confirmLogout) {
        ConfirmDialog("Abmelden?", "Deine Daten bleiben im Haushalt gespeichert.", confirm = "Abmelden",
            onConfirm = { scope.launch { AccountRepository.signOut(context) } }, onDismiss = { confirmLogout = false })
    }
}
