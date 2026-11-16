package de.unseralltag.data

import com.google.firebase.firestore.DocumentId

/*
 * Firestore-Modelle. Alle Felder sind `var` mit Standardwert, damit Firestore
 * die Objekte per Reflection befüllen kann. Datumsangaben ohne Uhrzeit werden
 * als "epochDay" (Tage seit 1970-01-01) gespeichert, Zeitpunkte als Millisekunden.
 */

data class UserProfile(
    var name: String = "",
    var email: String = "",
    var householdId: String? = null,
)

data class Household(
    @DocumentId var id: String = "",
    var name: String = "",
    var members: List<String> = emptyList(),
    var memberNames: Map<String, String> = emptyMap(),
    var inviteCode: String = "",
    var lastJoinCode: String = "",
)

object Recurrence {
    const val NONE = "NONE"
    const val DAILY = "DAILY"
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"
    const val YEARLY = "YEARLY"

    val labels = linkedMapOf(
        NONE to "Keine",
        DAILY to "Täglich",
        WEEKLY to "Wöchentlich",
        MONTHLY to "Monatlich",
        YEARLY to "Jährlich",
    )
}

data class CalendarEvent(
    @DocumentId var id: String = "",
    var title: String = "",
    var start: Long = 0,
    var end: Long = 0,
    var allDay: Boolean = false,
    var location: String = "",
    var notes: String = "",
    /** Minuten vor Beginn, -1 = keine Erinnerung */
    var reminderMinutes: Int = 60,
    /** "" = beide, sonst UID */
    var assignedTo: String = "",
    var recurrence: String = Recurrence.NONE,
    var createdBy: String = "",
)

object SpecialDayType {
    const val BIRTHDAY = "BIRTHDAY"
    const val ANNIVERSARY = "ANNIVERSARY"
    const val OTHER = "OTHER"
    val labels = linkedMapOf(
        BIRTHDAY to "Geburtstag",
        ANNIVERSARY to "Jahrestag",
        OTHER to "Sonstiges",
    )
}

data class SpecialDay(
    @DocumentId var id: String = "",
    var name: String = "",
    var type: String = SpecialDayType.BIRTHDAY,
    var day: Int = 1,
    var month: Int = 1,
    /** 0 = Jahr unbekannt */
    var year: Int = 0,
    var notes: String = "",
    /** Tage vorher erinnern, -1 = nur am Tag selbst */
    var reminderDays: Int = 7,
)

data class ShoppingList(
    @DocumentId var id: String = "",
    var name: String = "",
    var order: Long = 0,
)

data class ShoppingItem(
    @DocumentId var id: String = "",
    var listId: String = "",
    var name: String = "",
    var quantity: String = "",
    var checked: Boolean = false,
    var createdAt: Long = 0,
    var createdBy: String = "",
)

data class WishItem(
    @DocumentId var id: String = "",
    /** Für wen ist der Wunsch */
    var ownerUid: String = "",
    var title: String = "",
    var url: String = "",
    var price: String = "",
    var notes: String = "",
    var createdBy: String = "",
    /** Wer hat es schon besorgt – für den Wunsch-Besitzer unsichtbar */
    var purchasedBy: String = "",
    var createdAt: Long = 0,
)

data class TaskItem(
    @DocumentId var id: String = "",
    var title: String = "",
    var notes: String = "",
    var dueDay: Long? = null,
    var assignedTo: String = "",
    var done: Boolean = false,
    var doneAt: Long = 0,
    var recurrence: String = Recurrence.NONE,
    var createdAt: Long = 0,
)

data class Note(
    @DocumentId var id: String = "",
    var title: String = "",
    var content: String = "",
    var pinned: Boolean = false,
    var updatedAt: Long = 0,
    var updatedBy: String = "",
)

data class Meal(
    @DocumentId var id: String = "",
    var day: Long = 0,
    var title: String = "",
    var ingredients: List<String> = emptyList(),
    var notes: String = "",
)

object CostInterval {
    const val MONTHLY = "MONTHLY"
    const val QUARTERLY = "QUARTERLY"
    const val YEARLY = "YEARLY"
    const val ONCE = "ONCE"
    val labels = linkedMapOf(
        MONTHLY to "monatlich",
        QUARTERLY to "vierteljährlich",
        YEARLY to "jährlich",
        ONCE to "einmalig",
    )
}

object NoticeUnit {
    const val DAYS = "DAYS"
    const val WEEKS = "WEEKS"
    const val MONTHS = "MONTHS"
    val labels = linkedMapOf(DAYS to "Tage", WEEKS to "Wochen", MONTHS to "Monate")
}

data class Contract(
    @DocumentId var id: String = "",
    var name: String = "",
    var provider: String = "",
    var cost: Double = 0.0,
    var costInterval: String = CostInterval.MONTHLY,
    /** Vertragsende / nächste Verlängerung als epochDay */
    var endDay: Long? = null,
    var noticeValue: Int = 0,
    var noticeUnit: String = NoticeUnit.MONTHS,
    var reminderDays: Int = 14,
    var notes: String = "",
)
