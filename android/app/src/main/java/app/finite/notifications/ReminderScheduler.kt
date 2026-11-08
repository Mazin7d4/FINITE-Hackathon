package app.finite.notifications

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DueVideoReminder(val id: String, val videoCount: Int)

/** Persists counter milestones and atomically claims each one at most once per local day. */
class ReminderScheduler(
    context: Context,
    private val clock: () -> Date = { Date() },
) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

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

    companion object {
        private const val PREFERENCES = "finite_reminder"
        private const val KEY_IDS = "ids"
        private const val KEY_COUNT = "count"
        private const val KEY_LAST_SHOWN_DATE = "last_shown_date"
        private const val MIN_COUNT = 1
        private const val MAX_COUNT = 100
    }
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
