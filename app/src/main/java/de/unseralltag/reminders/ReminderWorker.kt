package de.unseralltag.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import de.unseralltag.MainActivity
import de.unseralltag.R
import de.unseralltag.data.AccountRepository
import de.unseralltag.data.CalendarEvent
import de.unseralltag.data.Collections
import de.unseralltag.data.Contract
import de.unseralltag.data.HouseholdStore
import de.unseralltag.data.SpecialDay
import de.unseralltag.data.TaskItem
import de.unseralltag.util.cancelDeadline
import de.unseralltag.util.daysUntil
import de.unseralltag.util.describe
import de.unseralltag.util.formatNumeric
import de.unseralltag.util.nextDate
import de.unseralltag.util.occurrences
import de.unseralltag.util.relativeDay
import de.unseralltag.util.timeLabel
import de.unseralltag.util.today
import de.unseralltag.util.toMillis
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Prüft regelmäßig (ca. alle 15 Minuten) auf anstehende Erinnerungen und zeigt lokale
 * Benachrichtigungen an. Bereits gemeldete Einträge werden gemerkt, damit nichts doppelt kommt.
 */
class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val uid = AccountRepository.currentUser?.uid ?: return Result.success()
        return try {
            val profile = Firebase.firestore.collection("users").document(uid).get().await()
            val hid = profile.getString("householdId") ?: return Result.success()
            val store = HouseholdStore(hid)
            val seen = Seen(applicationContext)
            checkEvents(store, uid, seen)
            if (LocalTime.now().hour >= 8) dailyDigest(store, uid, seen)
            seen.prune()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun checkEvents(store: HouseholdStore, uid: String, seen: Seen) {
        val now = LocalDateTime.now()
        val events = store.getAll(Collections.EVENTS, CalendarEvent::class.java)
            .filter { it.reminderMinutes >= 0 && (it.assignedTo.isBlank() || it.assignedTo == uid) }
        for (e in events) {
            // Vorkommen in den nächsten 8 Tagen (max. Vorlauf 1 Woche)
            for (o in occurrences(e, now.minusHours(1), now.plusDays(8))) {
                val trigger = o.start.minusMinutes(e.reminderMinutes.toLong())
                val key = "ev:${e.id}:${o.start.toMillis()}:${e.reminderMinutes}"
                if (!trigger.isAfter(now) && trigger.isAfter(now.minusHours(6)) && seen.add(key)) {
                    val day = relativeDay(o.start.toLocalDate())
                    notify(CHANNEL_EVENTS, key.hashCode(), e.title, "$day · ${o.timeLabel()}" +
                        if (e.location.isNotBlank()) " · ${e.location}" else "")
                }
            }
        }
    }

    private suspend fun dailyDigest(store: HouseholdStore, uid: String, seen: Seen) {
        val today = today()
        val dayKey = today.toString()

        store.getAll(Collections.SPECIAL_DAYS, SpecialDay::class.java).forEach { d ->
            val date = d.nextDate(today)
            val left = daysUntil(date, today)
            if (left == 0L || left == d.reminderDays.toLong()) {
                val key = "sd:${d.id}:$dayKey"
                if (seen.add(key)) {
                    val title = if (left == 0L) "🎉 Heute: ${d.name}" else "${d.name} – ${relativeDay(date, today)}"
                    notify(CHANNEL_DAYS, key.hashCode(), title, d.describe(date).ifBlank { date.formatNumeric() })
                }
            }
        }

        store.getAll(Collections.CONTRACTS, Contract::class.java).forEach { c ->
            val deadline = c.cancelDeadline() ?: return@forEach
            val left = daysUntil(deadline, today)
            if (left == c.reminderDays.toLong() || left == 3L || left == 0L) {
                val key = "ct:${c.id}:$dayKey"
                if (seen.add(key)) {
                    notify(CHANNEL_DAYS, key.hashCode(), "Kündigungsfrist: ${c.name}",
                        "Kündigen bis ${deadline.formatNumeric()} (${relativeDay(deadline, today)})")
                }
            }
        }

        val due = store.getAll(Collections.TASKS, TaskItem::class.java).filter {
            !it.done && it.dueDay != null && it.dueDay!! <= today.toEpochDay() &&
                (it.assignedTo.isBlank() || it.assignedTo == uid)
        }
        if (due.isNotEmpty()) {
            val key = "tasks:$dayKey"
            if (seen.add(key)) {
                notify(CHANNEL_DAYS, key.hashCode(),
                    if (due.size == 1) "1 Aufgabe fällig" else "${due.size} Aufgaben fällig",
                    due.joinToString(", ") { it.title })
            }
        }
    }

    private fun notify(channel: String, id: Int, title: String, text: String) {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val intent = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }

    /** Merkt sich gemeldete Erinnerungen (SharedPreferences). */
    private class Seen(ctx: Context) {
        private val prefs = ctx.getSharedPreferences("reminders_seen", Context.MODE_PRIVATE)
        fun add(key: String): Boolean {
            if (prefs.contains(key)) return false
            prefs.edit().putLong(key, System.currentTimeMillis()).apply()
            return true
        }
        fun prune() {
            val limit = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
            val old = prefs.all.filter { (it.value as? Long ?: 0L) < limit }.keys
            if (old.isNotEmpty()) prefs.edit().apply { old.forEach { remove(it) } }.apply()
        }
    }

    companion object {
        const val CHANNEL_EVENTS = "events"
        const val CHANNEL_DAYS = "days"
        private const val WORK_NAME = "reminders"

        fun createChannels(ctx: Context) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_EVENTS, "Termin-Erinnerungen", NotificationManager.IMPORTANCE_HIGH)
            )
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_DAYS, "Geburtstage, Aufgaben & Fristen", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, req)
        }

        fun runNow(ctx: Context) {
            WorkManager.getInstance(ctx).enqueue(OneTimeWorkRequestBuilder<ReminderWorker>().build())
        }
    }
}
