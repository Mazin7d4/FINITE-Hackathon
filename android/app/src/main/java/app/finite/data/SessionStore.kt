package app.finite.data

import android.content.Context
import android.content.Intent

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

class SessionStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    @Synchronized
    fun snapshot(): SessionState = SessionState(
        limit = preferences.getInt(KEY_LIMIT, DEFAULT_LIMIT).coerceIn(MIN_LIMIT, MAX_LIMIT),
        count = preferences.getInt(KEY_COUNT, 0).coerceAtLeast(0),
        active = preferences.getBoolean(KEY_ACTIVE, false),
        limitAcknowledged = preferences.getBoolean(KEY_ACKNOWLEDGED, false),
        detectorStatus = preferences.getString(KEY_STATUS, "Waiting for a session")
            ?: "Waiting for a session",
    )

    @Synchronized
    fun start(limit: Int): SessionState {
        val safeLimit = limit.coerceIn(MIN_LIMIT, MAX_LIMIT)
        preferences.edit()
            .putInt(KEY_LIMIT, safeLimit)
            .putInt(KEY_COUNT, 0)
            .putBoolean(KEY_ACTIVE, true)
            .putBoolean(KEY_ACKNOWLEDGED, false)
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
    fun increment(reason: String): CountResult {
        val before = snapshot()
        if (!before.active) return CountResult(before, false)

        val nextCount = before.count + 1
        val shouldShow = nextCount >= before.limit && !before.limitAcknowledged
        preferences.edit()
            .putInt(KEY_COUNT, nextCount)
            .putString(KEY_STATUS, "Counted video $nextCount · $reason")
            .apply()
        notifyChanged()
        return CountResult(snapshot(), shouldShow)
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

        private const val PREFERENCES = "finite_session"
        private const val KEY_LIMIT = "limit"
        private const val KEY_COUNT = "count"
        private const val KEY_ACTIVE = "active"
        private const val KEY_ACKNOWLEDGED = "acknowledged"
        private const val KEY_STATUS = "detector_status"
    }
}

