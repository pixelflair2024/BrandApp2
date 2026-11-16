package de.unseralltag

import android.app.Application
import de.unseralltag.reminders.ReminderWorker

class UnserAlltagApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderWorker.createChannels(this)
        ReminderWorker.schedule(this)
    }
}
