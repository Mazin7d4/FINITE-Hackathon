package app.finite.bridge

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import app.finite.accessibility.FiniteAccessibilityService
import app.finite.data.SessionState
import app.finite.data.SessionStore
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule

class FiniteNativeModule(
    private val reactContext: ReactApplicationContext,
) : ReactContextBaseJavaModule(reactContext) {
    private val store = SessionStore(reactContext)
    private var receiverRegistered = false
    private var listenerCount = 0

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
    fun startSession(limit: Double, promise: Promise) {
        val safeLimit = limit.toInt()
        if (safeLimit !in SessionStore.MIN_LIMIT..SessionStore.MAX_LIMIT) {
            promise.reject("INVALID_LIMIT", "Choose a Reel limit from 1 to 100")
            return
        }
        if (!isAccessibilityEnabled()) {
            promise.reject("ACCESSIBILITY_DISABLED", "FINITE accessibility access is disabled")
            return
        }
        val instagramIntent = reactContext.packageManager
            .getLaunchIntentForPackage(INSTAGRAM_PACKAGE)
        if (instagramIntent == null) {
            promise.reject("INSTAGRAM_MISSING", "Instagram is not installed")
            return
        }

        store.start(safeLimit)
        promise.resolve(stateMap())
        instagramIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        reactContext.startActivity(instagramIntent)
    }

    @ReactMethod
    fun endSession(promise: Promise) {
        store.end()
        promise.resolve(stateMap())
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

    companion object {
        private const val EVENT_STATE_CHANGED = "FiniteSessionChanged"
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"
        private const val TAG = "FINITE"
    }
}
