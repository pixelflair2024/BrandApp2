package de.unseralltag.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.unseralltag.ui.Nav
import de.unseralltag.ui.Routes
import de.unseralltag.ui.ScreenScaffold

private data class Entry(val title: String, val icon: ImageVector, val route: String)

private val entries = listOf(
    Entry("Geburtstage & Jahrestage", Icons.Default.Cake, Routes.SPECIAL_DAYS),
    Entry("Wunschlisten", Icons.Default.CardGiftcard, Routes.WISHLIST),
    Entry("Essensplan", Icons.Default.Restaurant, Routes.MEALS),
    Entry("Notizen", Icons.AutoMirrored.Filled.StickyNote2, Routes.NOTES),
    Entry("Verträge & Fristen", Icons.Default.Description, Routes.CONTRACTS),
    Entry("Einstellungen", Icons.Default.Settings, Routes.SETTINGS),
)

@Composable
fun MoreScreen(nav: Nav) {
    ScreenScaffold("Mehr") { pad ->
        LazyVerticalGrid(
            GridCells.Fixed(2),
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(entries) { e ->
                ElevatedCard(Modifier.fillMaxWidth().clickable { nav.go(e.route) }) {
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(e.icon, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(e.title, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}
