package de.unseralltag.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.unseralltag.data.AccountRepository
import de.unseralltag.data.Household
import de.unseralltag.data.HouseholdStore
import de.unseralltag.data.UserProfile
import kotlinx.coroutines.launch

private object Loading

@Composable
fun AppRoot() {
    val authFlow = remember { AccountRepository.authState() }
    val user by authFlow.collectAsState(initial = AccountRepository.currentUser)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val u = user
        if (u == null) {
            SignInScreen()
            return@Surface
        }
        val profileFlow = remember(u.uid) { AccountRepository.observeProfile(u.uid) }
        val profile by profileFlow.collectAsState(initial = Loading)
        when (val p = profile) {
            Loading -> LoadingBox()
            else -> {
                val hid = (p as? UserProfile)?.householdId
                if (hid.isNullOrBlank()) {
                    HouseholdSetupScreen(defaultName = (p as? UserProfile)?.name ?: (u.displayName ?: ""))
                } else {
                    val hhFlow = remember(hid) { AccountRepository.observeHousehold(hid) }
                    val household by hhFlow.collectAsState(initial = Loading)
                    when (val h = household) {
                        Loading -> LoadingBox()
                        is Household -> {
                            val store = remember(hid) { HouseholdStore(hid) }
                            val session = Session(u.uid, h, store)
                            CompositionLocalProvider(LocalSession provides session) {
                                MainScaffold()
                            }
                        }
                        else -> HouseholdSetupScreen(
                            defaultName = (p as? UserProfile)?.name ?: "",
                            hint = "Der gespeicherte Haushalt ist nicht erreichbar. Bitte neu erstellen oder beitreten.",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun SignInScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Favorite, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("UnserAlltag", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Termine, Geburtstage, Listen und mehr – gemeinsam organisiert.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            enabled = !busy,
            onClick = {
                busy = true; error = null
                scope.launch {
                    try {
                        AccountRepository.signInWithGoogle(context as Activity)
                    } catch (e: Exception) {
                        error = e.message ?: e.toString()
                    } finally {
                        busy = false
                    }
                }
            },
        ) { Text("Mit Google anmelden") }
        if (busy) {
            Spacer(Modifier.height(16.dp)); CircularProgressIndicator()
        }
        error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun HouseholdSetupScreen(defaultName: String, hint: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by rememberSaveable { mutableStateOf(0) }
    var myName by rememberSaveable { mutableStateOf(defaultName.substringBefore(' ')) }
    var householdName by rememberSaveable { mutableStateOf("Unser Zuhause") }
    var code by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Text("Willkommen!", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Erstellt einer von euch einen gemeinsamen Haushalt, der andere tritt mit dem Einladungscode bei.",
            style = MaterialTheme.typography.bodyLarge,
        )
        hint?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedTextField(
            value = myName, onValueChange = { myName = it },
            label = { Text("Dein Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(mode == 0, { mode = 0 }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("Neu erstellen") }
            SegmentedButton(mode == 1, { mode = 1 }, SegmentedButtonDefaults.itemShape(1, 2)) { Text("Beitreten") }
        }
        if (mode == 0) {
            OutlinedTextField(
                value = householdName, onValueChange = { householdName = it },
                label = { Text("Name des Haushalts") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
        } else {
            OutlinedTextField(
                value = code, onValueChange = { code = it.uppercase().take(6) },
                label = { Text("Einladungscode (6 Zeichen)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Button(
            enabled = !busy && myName.isNotBlank() && (if (mode == 0) householdName.isNotBlank() else code.length == 6),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                busy = true; error = null
                scope.launch {
                    try {
                        if (mode == 0) AccountRepository.createHousehold(householdName.trim(), myName.trim())
                        else AccountRepository.joinHousehold(code, myName.trim())
                    } catch (e: Exception) {
                        error = if (mode == 1) "Beitritt fehlgeschlagen – stimmt der Code? (${e.message})" else e.message
                    } finally {
                        busy = false
                    }
                }
            },
        ) { Text(if (mode == 0) "Haushalt erstellen" else "Beitreten") }
        if (busy) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        TextButton(onClick = { scope.launch { AccountRepository.signOut(context) } }) { Text("Abmelden") }
    }
}

