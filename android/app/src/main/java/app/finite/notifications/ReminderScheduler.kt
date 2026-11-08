package app.finite.notifications

import android.content.Context
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Intent
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DueVideoReminder(val id: String, val videoCount: Int)

/** Persists counter milestones and atomically claims each one at most once per local day. */
class ReminderScheduler(
    context: Context,
    private val clock: () -> Date = { Date() },
) {
    private val appContext = context.applicationContext
    private val prefs = appContext
        .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    init { migrateLegacyAlarms() }

    @Synchronized
    fun schedule(id: String, videoCount: Int) {
        prefs.edit()
            .putStringSet(KEY_IDS, ids() + id)
            .putInt(key(id, KEY_COUNT), videoCount.coerceIn(MIN_COUNT, MAX_COUNT))
            .apply()
    }

    @Synchronized
    fun cancel(id: String) {
        prefs.edit()
            .putStringSet(KEY_IDS, ids() - id)
            .remove(key(id, KEY_COUNT))
            .remove(key(id, KEY_LAST_SHOWN_DATE))
            .apply()
    }

    @Synchronized
    fun cancelAll() {
        ids().toList().forEach(::cancel)
    }

    /** Makes native state match the active JS profile without touching acknowledgements for retained IDs. */
    @Synchronized
    fun reconcile(reminders: Map<String, Int>) {
        val current = ids()
        val next = reminders.keys
        val editor = prefs.edit().putStringSet(KEY_IDS, next)
        (current - next).forEach { id ->
            editor.remove(key(id, KEY_COUNT)).remove(key(id, KEY_LAST_SHOWN_DATE))
        }
        reminders.forEach { (id, count) -> editor.putInt(key(id, KEY_COUNT), count.coerceIn(MIN_COUNT, MAX_COUNT)) }
        editor.commit()
    }

    /** Returns and marks the first crossed milestone. The hard daily limit always wins. */
    @Synchronized
    fun claimDueReminder(currentCount: Int, dailyLimit: Int): DueVideoReminder? {
        val today = dateKey(clock())
        val due = ids()
            .map { id -> DueVideoReminder(id, prefs.getInt(key(id, KEY_COUNT), MIN_COUNT)) }
            .filter { reminder ->
                ReminderThresholdPolicy.shouldShow(
                    threshold = reminder.videoCount,
                    currentCount = currentCount,
                    dailyLimit = dailyLimit,
                    lastShownDate = prefs.getString(key(reminder.id, KEY_LAST_SHOWN_DATE), null),
                    today = today,
                )
            }
            .minByOrNull(DueVideoReminder::videoCount)
            ?: return null

        prefs.edit().putString(key(due.id, KEY_LAST_SHOWN_DATE), today).apply()
        return due
    }

    private fun ids(): Set<String> = prefs.getStringSet(KEY_IDS, emptySet()).orEmpty()
    private fun key(id: String, suffix: String) = "reminder_${id}_$suffix"
    private fun dateKey(date: Date): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)

    /** PR21 used the legacy alarm preference file. Copy only milestone-shaped entries; discard timestamp alarms. */
    private fun migrateLegacyAlarms() {
        val legacy = appContext.getSharedPreferences(LEGACY_PREFERENCES, Context.MODE_PRIVATE)
        val legacyIds = legacy.getStringSet(KEY_IDS, emptySet()).orEmpty()
        if (legacyIds.isEmpty()) return
        val editor = prefs.edit()
        val migrated = ids().toMutableSet()
        legacyIds.forEach { id ->
            val hasAlarmFields = legacy.contains(key(id, KEY_AT)) || legacy.contains(key(id, KEY_REPEAT))
            if (!hasAlarmFields && legacy.contains(key(id, KEY_COUNT))) {
                migrated += id
                editor.putInt(key(id, KEY_COUNT), legacy.getInt(key(id, KEY_COUNT), MIN_COUNT))
                legacy.getString(key(id, KEY_LAST_SHOWN_DATE), null)?.let { editor.putString(key(id, KEY_LAST_SHOWN_DATE), it) }
            }
            cancelLegacyAlarm(id)
            editor.remove(key(id, KEY_AT)).remove(key(id, KEY_REPEAT))
        }
        editor.putStringSet(KEY_IDS, migrated).commit()
        legacy.edit().clear().apply()
    }

    private fun cancelLegacyAlarm(id: String) {
        val alarm = appContext.getSystemService(AlarmManager::class.java)
        val pendingIntent = PendingIntent.getBroadcast(appContext, REQUEST_CODE_BASE + (id.hashCode() and 0x7fff),
            Intent(appContext, ReminderReceiver::class.java).setData(Uri.parse("finite://reminder/$id")),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pendingIntent != null) {
            alarm.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    companion object {
        private const val PREFERENCES = "finite_milestone_reminder"
        private const val LEGACY_PREFERENCES = "finite_reminder"
        private const val KEY_IDS = "ids"
        private const val KEY_COUNT = "count"
        private const val KEY_LAST_SHOWN_DATE = "last_shown_date"
        private const val MIN_COUNT = 1
        private const val MAX_COUNT = 100
        private const val KEY_AT = "at"
        private const val KEY_REPEAT = "repeat"
        private const val REQUEST_CODE_BASE = 739
    }
}

/** Kept only so Android can identify and cancel pre-0.1.7 alarm PendingIntents during migration. */
@Deprecated("Legacy alarm cancellation target")
private class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) = Unit
}

internal object ReminderThresholdPolicy {
    fun shouldShow(
        threshold: Int,
        currentCount: Int,
        dailyLimit: Int,
        lastShownDate: String?,
        today: String,
    ): Boolean = threshold in 1 until dailyLimit &&
        currentCount >= threshold &&
        currentCount < dailyLimit &&
        lastShownDate != today
}
