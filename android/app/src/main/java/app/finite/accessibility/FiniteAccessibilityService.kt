package app.finite.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import app.finite.BuildConfig
import app.finite.MainActivity
import app.finite.data.SessionStore

class FiniteAccessibilityService : AccessibilityService() {
    private lateinit var store: SessionStore
    private lateinit var windowManager: WindowManager
    private val detector = ReelDetector()
    private val shortsDetector = ShortsDetector()
    private val shortsCounter = ShortsCounter()
    private val handler = Handler(Looper.getMainLooper())
    private var lastPackage: String? = null
    private var receiverRegistered = false
    private var lastShortsDiagnosticAt = 0L
    private val shortsFollowUp = Runnable {
        if (store.snapshot().active && lastPackage == YOUTUBE_PACKAGE && overlay == null) {
            handleShorts(null)
        }
    }
    private val lifecycleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val explicitReset = intent?.action == ACTION_RESET_DETECTION
            val sessionEnded = intent?.action == SessionStore.ACTION_STATE_CHANGED && !store.snapshot().active
            if (explicitReset || sessionEnded) {
                resetDetectionState()
                dismissOverlay()
                if (BuildConfig.DEBUG) Log.d("FINITE_SHORTS", "session reset; both detectors cleared")
            }
        }
    }

    private var overlay: View? = null
    private var wasInViewer = false
    private var lastFingerprint: String? = null
    private var pendingFingerprint: String? = null
    private var pendingFingerprintHits = 0
    private var lastCountAt = 0L
    private var lastDiagnosticAt = 0L

    override fun onCreate() {
        super.onCreate()
        store = SessionStore(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        ContextCompat.registerReceiver(this, lifecycleReceiver,
            IntentFilter(ACTION_RESET_DETECTION).apply { addAction(SessionStore.ACTION_STATE_CHANGED) },
            ContextCompat.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility service connected")
        store.updateDetectorStatus("Accessibility ready")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString()
        if (packageName != INSTAGRAM_PACKAGE && packageName != YOUTUBE_PACKAGE) return
        val session = store.snapshot()
        if (!session.active) {
            resetDetectionState()
            return
        }

        if (lastPackage != packageName) {
            handler.removeCallbacks(shortsFollowUp)
            shortsCounter.suspend()
            pendingFingerprint = null
            pendingFingerprintHits = 0
            lastPackage = packageName
        }
        if (packageName == YOUTUBE_PACKAGE) {
            handleShorts(event)
            return
        }

        val detection = detector.inspect(event, rootInActiveWindow)
        logDiagnostic(event, detection)

        if (!detection.isReelsViewer) {
            if (wasInViewer) {
                store.updateDetectorStatus("Instagram open — outside the Reels viewer")
            }
            wasInViewer = false
            pendingFingerprint = null
            pendingFingerprintHits = 0
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (!wasInViewer) {
            wasInViewer = true
            lastFingerprint = detection.fingerprint
            pendingFingerprint = null
            pendingFingerprintHits = 0
            if (detection.strongEvidence) {
                countReel("Reels viewer opened", now)
            } else {
                store.updateDetectorStatus("Reels viewer candidate — confirming identity")
            }
            return
        }

        val primaryScrollSignal = event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED &&
            detection.sourceLooksScrollable
        if (primaryScrollSignal && now - lastCountAt >= SCROLL_DEBOUNCE_MS) {
            lastFingerprint = detection.fingerprint ?: lastFingerprint
            pendingFingerprint = null
            pendingFingerprintHits = 0
            countReel("vertical Reel transition", now)
            return
        }

        val fingerprint = detection.fingerprint ?: return
        if (fingerprint == lastFingerprint) {
            pendingFingerprint = null
            pendingFingerprintHits = 0
            return
        }

        if (pendingFingerprint == fingerprint) {
            pendingFingerprintHits += 1
        } else {
            pendingFingerprint = fingerprint
            pendingFingerprintHits = 1
        }

        if (pendingFingerprintHits >= FINGERPRINT_CONFIRMATIONS &&
            now - lastCountAt >= FINGERPRINT_DEBOUNCE_MS
        ) {
            lastFingerprint = fingerprint
            pendingFingerprint = null
            pendingFingerprintHits = 0
            countReel("confirmed Reel identity change", now)
        }
    }

    private fun countReel(reason: String, now: Long) {
        lastCountAt = now
        recordCount(reason)
    }

    private fun handleShorts(event: AccessibilityEvent?) {
        handler.removeCallbacks(shortsFollowUp)
        if (!store.snapshot().active || overlay != null) {
            shortsCounter.suspend()
            return
        }
        val observation = shortsDetector.inspect(event, rootInActiveWindow)
        val now = SystemClock.elapsedRealtime()
        val decision = shortsCounter.update(observation, now)
        if (BuildConfig.DEBUG && (decision.incrementReason != null ||
                event?.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
                now - lastShortsDiagnosticAt >= 500L)
        ) {
            lastShortsDiagnosticAt = now
            Log.d("FINITE_SHORTS", "t=$now event=${event?.let {
                AccessibilityEvent.eventTypeToString(it.eventType)
            } ?: "FOLLOW_UP"} viewer=${observation.viewer} blocked=${observation.blocked} " +
                "scroll=${observation.scroll} decision=${decision.detail} ${observation.summary}")
        }
        decision.incrementReason?.let(::recordCount)
        if (overlay == null) shortsCounter.nextCheckDelay(now)?.let { handler.postDelayed(shortsFollowUp, it) }
    }

    private fun recordCount(reason: String) {
        val result = store.increment(reason)
        Log.i(TAG, "Count=${result.state.count}/${result.state.limit}; reason=$reason")
        if (result.shouldShowLimit && overlay == null) showLimitOverlay(result.state.limit)
    }

    private fun logDiagnostic(event: AccessibilityEvent, detection: Detection) {
        val now = SystemClock.elapsedRealtime()
        val important = event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
            detection.isReelsViewer
        if (!important && now - lastDiagnosticAt < DIAGNOSTIC_THROTTLE_MS) return
        lastDiagnosticAt = now
        Log.d(
            TAG,
            "event=${AccessibilityEvent.eventTypeToString(event.eventType)} " +
                "class=${event.className} ${detection.summary}",
        )
    }

    private fun showLimitOverlay(limit: Int) {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(28), dp(28), dp(24))
            background = GradientDrawable().apply {
                setColor(Color.rgb(20, 21, 23))
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), Color.rgb(63, 65, 68))
            }
            elevation = dp(16).toFloat()
        }

        panel.addView(TextView(this).apply {
            text = "YOU’VE REACHED YOUR LIMIT"
            setTextColor(Color.rgb(217, 255, 87))
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
        }, matchWrap(top = 0, bottom = 12))

        panel.addView(TextView(this).apply {
            text = "You planned to watch $limit Reels and Shorts. You’ve reached your goal!"
            setTextColor(Color.WHITE)
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
        }, matchWrap(top = 0, bottom = 22))

        panel.addView(overlayButton("I’m Done", primary = true) {
            dismissOverlay()
            store.end("Goal complete — session ended")
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            )
        }, matchWrap(top = 0, bottom = 10))

        panel.addView(overlayButton("Continue Anyway", primary = false) {
            store.acknowledgeLimit()
            dismissOverlay()
        }, matchWrap(top = 0, bottom = 0))

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
            horizontalMargin = 0.06f
        }

        try {
            windowManager.addView(panel, params)
            overlay = panel
            Log.i(TAG, "Limit overlay shown")
        } catch (error: RuntimeException) {
            Log.e(TAG, "Unable to show accessibility overlay", error)
            store.updateDetectorStatus("Limit reached, but overlay failed — see FINITE logs")
        }
    }

    private fun overlayButton(label: String, primary: Boolean, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 16f
            isAllCaps = false
            setTextColor(if (primary) Color.rgb(9, 10, 11) else Color.WHITE)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(if (primary) Color.rgb(217, 255, 87) else Color.rgb(42, 43, 46))
                cornerRadius = dp(14).toFloat()
            }
            setOnClickListener { action() }
        }

    private fun matchWrap(top: Int, bottom: Int) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply {
        topMargin = dp(top)
        bottomMargin = dp(bottom)
    }

    private fun dismissOverlay() {
        overlay?.let {
            try {
                windowManager.removeView(it)
            } catch (_: RuntimeException) {
                // The system may already have removed an accessibility window.
            }
        }
        overlay = null
    }

    private fun resetDetectionState() {
        handler.removeCallbacks(shortsFollowUp)
        shortsCounter.reset()
        lastPackage = null
        wasInViewer = false
        lastFingerprint = null
        pendingFingerprint = null
        pendingFingerprintHits = 0
        lastCountAt = 0L
    }

    override fun onInterrupt() {
        handler.removeCallbacks(shortsFollowUp)
        shortsCounter.suspend()
        Log.w(TAG, "Accessibility service interrupted")
        store.updateDetectorStatus("Accessibility interrupted — reopen FINITE if counting stops")
    }

    override fun onDestroy() {
        handler.removeCallbacks(shortsFollowUp)
        if (receiverRegistered) {
            unregisterReceiver(lifecycleReceiver)
            receiverRegistered = false
        }
        dismissOverlay()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val ACTION_RESET_DETECTION = "app.finite.RESET_DETECTION"
        private const val TAG = "FINITE"
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"
        private const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        private const val SCROLL_DEBOUNCE_MS = 700L
        private const val FINGERPRINT_DEBOUNCE_MS = 1_800L
        private const val FINGERPRINT_CONFIRMATIONS = 2
        private const val DIAGNOSTIC_THROTTLE_MS = 1_500L
    }
}

