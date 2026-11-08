package app.finite.bridge

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Handler
import android.provider.Settings
import android.util.Log
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.Executors
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import androidx.core.content.ContextCompat
import app.finite.accessibility.FiniteAccessibilityService
import app.finite.data.SessionState
import app.finite.data.SessionStore
import app.finite.notifications.ReminderScheduler
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.modules.core.DeviceEventManagerModule

class FiniteNativeModule(
    private val reactContext: ReactApplicationContext,
) : ReactContextBaseJavaModule(reactContext) {
    private val store = SessionStore(reactContext)
    private val reminders = ReminderScheduler(reactContext)
    private var receiverRegistered = false
    private var listenerCount = 0
    private val cryptoExecutor = Executors.newFixedThreadPool(1)

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (listenerCount > 0 && reactContext.hasActiveReactInstance()) {
                reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    .emit(EVENT_STATE_CHANGED, stateMap())
            }
        }
    }

    override fun getName(): String = "FiniteNative"

    override fun initialize() {
        super.initialize()
        ContextCompat.registerReceiver(
            reactContext,
            stateReceiver,
            IntentFilter(SessionStore.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    override fun invalidate() {
        if (receiverRegistered) {
            reactContext.unregisterReceiver(stateReceiver)
            receiverRegistered = false
        }
        super.invalidate()
    }

    @ReactMethod
    fun getState(promise: Promise) {
        promise.resolve(stateMap())
    }

    @ReactMethod
    fun configure(limit: Double, selectedApps: ReadableArray, days: ReadableArray, startMinute: Double, endMinute: Double, promise: Promise) {
        val apps = (0 until selectedApps.size()).mapNotNull { selectedApps.getString(it) }.toSet()
        val dayMask = if ((0 until days.size()).all { days.getInt(it) in 0..6 }) {
            (0 until days.size()).fold(0) { mask, index -> mask or (1 shl days.getInt(index)) }
        } else {
            -1
        }
        store.configure(limit.toInt(), apps, dayMask, startMinute.toInt(), endMinute.toInt())
        promise.resolve(stateMap())
    }

    @ReactMethod
    fun getUsage(promise: Promise) {
        val today = store.history().last()
        val history = Arguments.createArray()
        store.history().forEach { day ->
            history.pushMap(Arguments.createMap().apply {
                putString("date", day.date)
                putInt("count", day.count)
                putInt("instagram", day.instagram)
                putInt("youtube", day.youtube)
            })
        }
        promise.resolve(Arguments.createMap().apply {
            putInt("todayCount", today.count)
            putMap("platformCounts", Arguments.createMap().apply {
                putInt("instagram", today.instagram)
                putInt("youtube", today.youtube)
            })
            putArray("history", history)
        })
    }

    @ReactMethod
    fun startSession(limit: Double, promise: Promise) {
        val safeLimit = limit.toInt()
        if (safeLimit !in SessionStore.MIN_LIMIT..SessionStore.MAX_LIMIT) {
            promise.reject("INVALID_LIMIT", "Choose a Reels and Shorts limit from 1 to 100")
            return
        }
        if (!isAccessibilityEnabled()) {
            promise.reject("ACCESSIBILITY_DISABLED", "FINITE accessibility access is disabled")
            return
        }
        val configuredApps = store.schedule().selectedApps
        val launchPackage = selectLaunchPackage(configuredApps) { packageName ->
            reactContext.packageManager.getLaunchIntentForPackage(packageName) != null
        }
        val launchIntent = launchPackage?.let(reactContext.packageManager::getLaunchIntentForPackage)
        if (launchIntent == null) {
            promise.reject("SUPPORTED_APP_MISSING", "Neither Instagram nor YouTube is installed")
            return
        }

        // Wait for the service's explicit reset receiver before starting the new session.
        // A service created later already starts with fresh detector state.
        reactContext.sendOrderedBroadcast(
            Intent(FiniteAccessibilityService.ACTION_RESET_DETECTION).setPackage(reactContext.packageName),
            null,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
            store.start(safeLimit)
                    try {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        reactContext.startActivity(launchIntent)
                        promise.resolve(stateMap())
                    } catch (error: RuntimeException) {
                        store.end("Unable to launch Instagram or YouTube")
                        promise.reject("APP_LAUNCH_FAILED", "Unable to launch the selected app", error)
                    }
                }
            },
            Handler(reactContext.mainLooper), 0, null, null,
        )
    }

    @ReactMethod
    fun endSession(promise: Promise) {
        store.end()
        promise.resolve(stateMap())
    }

    /** Ends the native counter session and cancels its persisted alarms. */
    @ReactMethod
    fun cancelAllReminders(promise: Promise) {
        reminders.cancelAll()
        promise.resolve(true)
    }

    /** Derives a password verifier without doing PBKDF2 work on the UI thread. */
    @ReactMethod
    fun hashPassword(password: String, promise: Promise) {
        cryptoExecutor.execute {
            try {
                val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
                val hash = derive(password, salt, ITERATIONS)
                promise.resolve(Arguments.createMap().apply {
                    putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                    putString("hash", Base64.encodeToString(hash, Base64.NO_WRAP))
                    putInt("iterations", ITERATIONS)
                })
            } catch (error: Exception) { promise.reject("PASSWORD_HASH_FAILED", "Unable to protect this local profile", error) }
        }
    }

    /** Verifies a password with constant-time comparison off the UI thread. */
    @ReactMethod
    fun verifyPassword(password: String, saltText: String, hashText: String, iterations: Double, promise: Promise) {
        cryptoExecutor.execute {
            try {
                val expected = Base64.decode(hashText, Base64.DEFAULT)
                val actual = derive(password, Base64.decode(saltText, Base64.DEFAULT), iterations.toInt().coerceIn(MIN_ITERATIONS, MAX_ITERATIONS))
                promise.resolve(MessageDigest.isEqual(actual, expected))
            } catch (_: Exception) { promise.resolve(false) }
        }
    }

    @ReactMethod
    fun requestNotificationPermission(promise: Promise) {
        if (android.os.Build.VERSION.SDK_INT < 33 ||
            androidx.core.content.ContextCompat.checkSelfPermission(reactContext, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            promise.resolve(true)
            return
        }
        val activity = reactContext.currentActivity
        if (activity == null) {
            promise.reject("NO_ACTIVITY", "FINITE needs an active Android screen to request notification permission")
            return
        }
        activity.requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_REQUEST_CODE)
        promise.resolve(false)
    }

    @ReactMethod
    fun scheduleReminder(id: String, atMillis: Double, videoCount: Double, repeat: String, promise: Promise) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(reactContext, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            promise.reject("NOTIFICATION_PERMISSION_REQUIRED", "Allow notifications, then save the reminder again")
            return
        }
        if (atMillis <= System.currentTimeMillis()) {
            promise.reject("INVALID_REMINDER_TIME", "Choose a future reminder time")
            return
        }
        reminders.schedule(id, atMillis.toLong(), videoCount.toInt().coerceIn(1, 100), repeat)
        promise.resolve(true)
    }

    @ReactMethod
    fun cancelReminder(id: String, promise: Promise) {
        reminders.cancel(id)
        promise.resolve(true)
    }

    @ReactMethod
    fun pickReminderDateTime(initialTimestamp: Double, promise: Promise) {
        val activity = reactContext.currentActivity
        if (activity == null) {
            promise.reject("NO_ACTIVITY", "FINITE needs an active Android screen to choose a reminder time")
            return
        }

        activity.runOnUiThread {
            val settled = AtomicBoolean(false)
            fun cancel() { if (settled.compareAndSet(false, true)) promise.resolve(null) }
            fun finish(timestamp: Long) { if (settled.compareAndSet(false, true)) promise.resolve(timestamp.toDouble()) }
            val initial = Calendar.getInstance().apply { timeInMillis = initialTimestamp.toLong() }
            val showTimePicker = { year: Int, month: Int, day: Int ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, day, initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE), 0)
                }
                TimePickerDialog(activity, { _, hour, minute ->
                    selected.set(Calendar.HOUR_OF_DAY, hour)
                    selected.set(Calendar.MINUTE, minute)
                    finish(selected.timeInMillis)
                }, selected.get(Calendar.HOUR_OF_DAY), selected.get(Calendar.MINUTE), android.text.format.DateFormat.is24HourFormat(activity)).apply {
                    setOnCancelListener { cancel() }
                }.show()
            }
            DatePickerDialog(activity, { _, year, month, day -> showTimePicker(year, month, day) }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).apply {
                setOnCancelListener { cancel() }
            }.show()
        }
    }

    @ReactMethod
    fun pickTime(initialMinute: Double, promise: Promise) {
        val activity = reactContext.currentActivity
        if (activity == null) {
            promise.reject("NO_ACTIVITY", "FINITE needs an active Android screen to choose a time")
            return
        }

        activity.runOnUiThread {
            val settled = AtomicBoolean(false)
            fun cancel() { if (settled.compareAndSet(false, true)) promise.resolve(null) }
            fun finish(minute: Int) { if (settled.compareAndSet(false, true)) promise.resolve(minute.toDouble()) }
            val safeMinute = initialMinute.toInt().coerceIn(0, 23 * 60 + 59)
            TimePickerDialog(
                activity,
                { _, hour, minute -> finish(hour * 60 + minute) },
                safeMinute / 60,
                safeMinute % 60,
                true,
            ).apply {
                setOnCancelListener { cancel() }
            }.show()
        }
    }

    @ReactMethod
    fun openAccessibilitySettings(promise: Promise) {
        val candidates = listOf(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
        var lastFailure: RuntimeException? = null

        for (candidate in candidates) {
            candidate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (candidate.resolveActivity(reactContext.packageManager) == null) continue
            try {
                reactContext.startActivity(candidate)
                promise.resolve(null)
                return
            } catch (error: RuntimeException) {
                lastFailure = error
                Log.w(TAG, "Unable to open ${candidate.action}", error)
            }
        }

        promise.reject(
            "SETTINGS_UNAVAILABLE",
            "Android accessibility settings are unavailable on this device",
            lastFailure,
        )
    }

    @ReactMethod
    fun addListener(eventName: String) {
        if (eventName == EVENT_STATE_CHANGED) listenerCount += 1
    }

    @ReactMethod
    fun removeListeners(count: Double) {
        listenerCount = (listenerCount - count.toInt()).coerceAtLeast(0)
    }

    private fun stateMap(state: SessionState = store.snapshot()): WritableMap =
        Arguments.createMap().apply {
            putInt("limit", state.limit)
            putInt("count", state.count)
            putBoolean("active", state.active)
            putBoolean("limitAcknowledged", state.limitAcknowledged)
            putString("detectorStatus", state.detectorStatus)
            putBoolean("accessibilityEnabled", isAccessibilityEnabled())
            val config = store.schedule()
            putArray("selectedApps", Arguments.createArray().apply { config.selectedApps.forEach(::pushString) })
            putInt("startMinute", config.startMinute)
            putInt("endMinute", config.endMinute)
        }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getInt(
            reactContext.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0,
        ) == 1
        if (!enabled) return false

        val expected = ComponentName(reactContext, FiniteAccessibilityService::class.java)
        return Settings.Secure.getString(
            reactContext.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
            .split(':')
            .mapNotNull(ComponentName::unflattenFromString)
            .any { it == expected }
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS)).encoded

    companion object {
        private const val EVENT_STATE_CHANGED = "FiniteSessionChanged"
        private const val TAG = "FINITE"
        private const val NOTIFICATION_REQUEST_CODE = 9031
        private const val SALT_BYTES = 16
        private const val HASH_BITS = 256
        private const val ITERATIONS = 120_000
        private const val MIN_ITERATIONS = 100_000
        private const val MAX_ITERATIONS = 500_000
    }
}
