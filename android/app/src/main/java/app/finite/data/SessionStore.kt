package app.finite.data

import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.json.JSONObject

data class SessionState(
    val limit: Int,
    val count: Int,
    val active: Boolean,
    val limitAcknowledged: Boolean,
    val detectorStatus: String,
)

data class CountResult(
    val state: SessionState,
    val shouldShowLimit: Boolean,
)

data class UsageDay(val date: String, val count: Int, val instagram: Int, val youtube: Int)

data class ScheduleConfig(
    val selectedApps: Set<String> = setOf(SessionStore.INSTAGRAM, SessionStore.YOUTUBE),
    val daysMask: Int = SessionStore.ALL_DAYS_MASK,
    val startMinute: Int = 8 * 60,
    val endMinute: Int = 22 * 60,
)

class SessionStore(
    context: Context,
    private val clock: () -> Date = { Date() },
) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    @Synchronized
    fun snapshot(): SessionState {
        resetForLocalDateRollover()
        return SessionState(
            limit = preferences.getInt(KEY_LIMIT, DEFAULT_LIMIT).coerceIn(MIN_LIMIT, MAX_LIMIT),
            count = todayUsage().count,
            active = preferences.getBoolean(KEY_ACTIVE, false),
            limitAcknowledged = preferences.getBoolean(KEY_ACKNOWLEDGED, false),
            detectorStatus = preferences.getString(KEY_STATUS, "Waiting for a session")
                ?: "Waiting for a session",
        )
    }

    @Synchronized
    fun configure(limit: Int, selectedApps: Set<String>, daysMask: Int, startMinute: Int, endMinute: Int) {
        preferences.edit()
            .putInt(KEY_LIMIT, limit.coerceIn(MIN_LIMIT, MAX_LIMIT))
            .putStringSet(KEY_APPS, selectedApps.intersect(SUPPORTED_APPS))
            .putInt(KEY_DAYS, daysMask and ALL_DAYS_MASK)
            .putInt(KEY_START, startMinute.coerceIn(0, MINUTES_PER_DAY - 1))
            .putInt(KEY_END, endMinute.coerceIn(0, MINUTES_PER_DAY - 1))
            .apply()
        notifyChanged()
    }

    @Synchronized
    fun schedule(): ScheduleConfig = ScheduleConfig(
        selectedApps = preferences.getStringSet(KEY_APPS, SUPPORTED_APPS)?.intersect(SUPPORTED_APPS) ?: SUPPORTED_APPS,
        daysMask = preferences.getInt(KEY_DAYS, ALL_DAYS_MASK) and ALL_DAYS_MASK,
        startMinute = preferences.getInt(KEY_START, 8 * 60).coerceIn(0, MINUTES_PER_DAY - 1),
        endMinute = preferences.getInt(KEY_END, 22 * 60).coerceIn(0, MINUTES_PER_DAY - 1),
    )

    @Synchronized
    fun start(limit: Int): SessionState {
        val safeLimit = limit.coerceIn(MIN_LIMIT, MAX_LIMIT)
        preferences.edit()
            .putInt(KEY_LIMIT, safeLimit)
            .putBoolean(KEY_ACTIVE, true)
            .putBoolean(KEY_ACKNOWLEDGED, false)
            .putString(KEY_SESSION_DATE, todayKey())
            .putString(KEY_STATUS, "Session active — waiting for Reels or Shorts")
            .apply()
        notifyChanged()
        return snapshot()
    }

    @Synchronized
    fun end(status: String = "Session ended"): SessionState {
        preferences.edit()
            .putBoolean(KEY_ACTIVE, false)
            .putString(KEY_STATUS, status)
            .apply()
        notifyChanged()
        return snapshot()
    }

    @Synchronized
    fun acknowledgeLimit(): SessionState {
        preferences.edit()
            .putBoolean(KEY_ACKNOWLEDGED, true)
            .putString(KEY_STATUS, "Limit acknowledged — still counting")
            .apply()
        notifyChanged()
        return snapshot()
    }

    @Synchronized
    fun increment(packageName: String, reason: String): CountResult {
        val before = snapshot()
        if (!before.active || packageName !in schedule().selectedApps || !isWithinSchedule()) {
            return CountResult(before, false)
        }
        val current = todayUsage()
        val next = UsageDay(
            todayKey(), current.count + 1,
            current.instagram + if (packageName == INSTAGRAM) 1 else 0,
            current.youtube + if (packageName == YOUTUBE) 1 else 0,
        )
        saveUsage(next)
        val shouldShow = next.count >= before.limit && !before.limitAcknowledged
        preferences.edit().putString(KEY_STATUS, "Counted video ${next.count} · $reason").apply()
        notifyChanged()
        return CountResult(snapshot(), shouldShow)
    }

    @Synchronized
    fun increment(reason: String): CountResult {
        val before = snapshot()
        if (!before.active) return CountResult(before, false)
        val current = todayUsage()
        val next = UsageDay(todayKey(), current.count + 1, current.instagram, current.youtube)
        saveUsage(next)
        val shouldShow = next.count >= before.limit && !before.limitAcknowledged
        preferences.edit().putString(KEY_STATUS, "Counted video ${next.count} · $reason").apply()
        notifyChanged()
        return CountResult(snapshot(), shouldShow)
    }

    /** Final policy check for UI side effects; overlays must never escape app or schedule policy. */
    @Synchronized
    fun canShowLimitFor(packageName: String): Boolean {
        val appId = when (packageName) {
            INSTAGRAM, INSTAGRAM_PACKAGE -> INSTAGRAM
            YOUTUBE, YOUTUBE_PACKAGE -> YOUTUBE
            else -> return false
        }
        return appId in schedule().selectedApps && isWithinSchedule()
    }

    @Synchronized
    fun history(): List<UsageDay> {
        val result = mutableListOf<UsageDay>()
        val calendar = currentCalendar()
        repeat(7) {
            result += usageFor(dateKey(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return result.reversed()
    }

    @Synchronized
    fun isWithinSchedule(now: Calendar = currentCalendar()): Boolean {
        val config = schedule()
        val minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val overnight = config.startMinute > config.endMinute
        val scheduleDay = (now.clone() as Calendar).apply {
            if (overnight && minute < config.endMinute) add(Calendar.DAY_OF_YEAR, -1)
        }.get(Calendar.DAY_OF_WEEK)
        val dayBit = 1 shl (scheduleDay - Calendar.SUNDAY)
        if (config.daysMask and dayBit == 0) return false
        return if (config.startMinute == config.endMinute) true
        else if (!overnight) minute in config.startMinute until config.endMinute
        else minute >= config.startMinute || minute < config.endMinute
    }

    private fun todayUsage(): UsageDay = usageFor(todayKey())

    private fun usageFor(date: String): UsageDay {
        val history = try { JSONObject(preferences.getString(KEY_HISTORY, "{}").orEmpty()) } catch (_: Exception) { JSONObject() }
        val value = history.optJSONObject(date) ?: return UsageDay(date, 0, 0, 0)
        return UsageDay(date, value.optInt("count"), value.optInt(INSTAGRAM), value.optInt(YOUTUBE))
    }

    private fun saveUsage(day: UsageDay) {
        val history = try { JSONObject(preferences.getString(KEY_HISTORY, "{}").orEmpty()) } catch (_: Exception) { JSONObject() }
        history.put(day.date, JSONObject().put("count", day.count).put(INSTAGRAM, day.instagram).put(YOUTUBE, day.youtube))
        preferences.edit().putString(KEY_HISTORY, history.toString()).putInt(KEY_COUNT, day.count).apply()
    }

    private fun todayKey(): String = dateKey(clock())
    private fun dateKey(date: Date): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)

    private fun currentCalendar(): Calendar = Calendar.getInstance().apply { time = clock() }

    /** Daily usage rolls over independently from the session, which may remain active overnight. */
    private fun resetForLocalDateRollover() {
        if (!preferences.getBoolean(KEY_ACTIVE, false)) return
        val today = todayKey()
        val sessionDate = preferences.getString(KEY_SESSION_DATE, null)
        if (sessionDate == null) {
            preferences.edit().putString(KEY_SESSION_DATE, today).apply()
            return
        }
        if (sessionDate == today) return
        preferences.edit()
            .putString(KEY_SESSION_DATE, today)
            .putBoolean(KEY_ACKNOWLEDGED, false)
            .putString(KEY_STATUS, "Session active — waiting for Reels or Shorts")
            .apply()
    }

    @Synchronized
    fun updateDetectorStatus(status: String) {
        // Keep the last counted source/reason visible on the counter screen. Detector chatter
        // must not replace it with a later transient rejection or outside-viewer message.
        if (preferences.getString(KEY_STATUS, null)?.startsWith("Counted video ") == true) return
        if (preferences.getString(KEY_STATUS, null) == status) return
        preferences.edit().putString(KEY_STATUS, status).apply()
        notifyChanged()
    }

    private fun notifyChanged() {
        appContext.sendBroadcast(
            Intent(ACTION_STATE_CHANGED).setPackage(appContext.packageName),
        )
    }

    companion object {
        const val ACTION_STATE_CHANGED = "app.finite.SESSION_STATE_CHANGED"
        const val MIN_LIMIT = 1
        const val MAX_LIMIT = 100
        const val DEFAULT_LIMIT = 5
        const val MINUTES_PER_DAY = 24 * 60
        const val INSTAGRAM = "instagram"
        const val YOUTUBE = "youtube"
        const val UNKNOWN_APP = "unknown"
        const val INSTAGRAM_PACKAGE = "com.instagram.android"
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        val SUPPORTED_APPS = setOf(INSTAGRAM, YOUTUBE)
        const val ALL_DAYS_MASK = 0x7f

        private const val PREFERENCES = "finite_session"
        private const val KEY_LIMIT = "limit"
        private const val KEY_COUNT = "count"
        private const val KEY_ACTIVE = "active"
        private const val KEY_ACKNOWLEDGED = "acknowledged"
        private const val KEY_SESSION_DATE = "session_date"
        private const val KEY_STATUS = "detector_status"
        private const val KEY_HISTORY = "daily_history"
        private const val KEY_APPS = "selected_apps"
        private const val KEY_DAYS = "schedule_days"
        private const val KEY_START = "schedule_start"
        private const val KEY_END = "schedule_end"
    }
}

