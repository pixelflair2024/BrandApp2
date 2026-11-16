package de.unseralltag.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import de.unseralltag.data.Household
import de.unseralltag.data.HouseholdStore
import de.unseralltag.ui.theme.BothColor
import de.unseralltag.ui.theme.PersonColors

/** Alles, was die Screens über den angemeldeten Haushalt wissen müssen. */
data class Session(
    val myUid: String,
    val household: Household,
    val store: HouseholdStore,
) {
    val members: List<String> get() = household.members
    fun nameOf(uid: String): String = when {
        uid.isBlank() -> "Beide"
        else -> household.memberNames[uid] ?: "?"
    }
    val myName: String get() = nameOf(myUid)
    val partnerUid: String? get() = members.firstOrNull { it != myUid }
    fun colorOf(uid: String): Color {
        if (uid.isBlank()) return BothColor
        val i = members.indexOf(uid)
        return if (i >= 0) PersonColors[i % PersonColors.size] else Color.Gray
    }
}

val LocalSession = staticCompositionLocalOf<Session> { error("Keine Session") }
