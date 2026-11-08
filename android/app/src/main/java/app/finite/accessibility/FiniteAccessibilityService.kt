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
import app.finite.MainActivity
import app.finite.data.SessionStore

class FiniteAccessibilityService : AccessibilityService() {
    private lateinit var store: SessionStore
    private lateinit var windowManager: WindowManager
    private val detector = ReelDetector()
    private val reelCounter = ReelCounter()
    private val shortsDetector = ShortsDetector()
    private val shortsCounter = ShortsCounter()
    private val handler = Handler(Looper.getMainLooper())
    private var lastPackage: String? = null
    private var receiverRegistered = false
    private var lastShortsDiagnosticAt = 0L
    private val reelFollowUp = Runnable {
        if (store.snapshot().active && lastPackage == INSTAGRAM_PACKAGE && overlay == null) {
            flushReels()
        }
    }
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
                Log.d(TAG, "session reset; both detectors cleared")
            }
        }
    }

    private var overlay: View? = null
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
        if (!session.active || overlay != null) {
            if (!session.active) resetDetectionState()
            return
        }

        if (lastPackage != packageName) {
            handler.removeCallbacks(shortsFollowUp)
            handler.removeCallbacks(reelFollowUp)
            shortsCounter.suspend()
            if (lastPackage == INSTAGRAM_PACKAGE) {
                reelCounter.platformSuspend(SystemClock.elapsedRealtime())
            }
            lastPackage = packageName
        }
        if (packageName == YOUTUBE_PACKAGE) {
            handleShorts(event)
            return
        }

        handler.removeCallbacks(reelFollowUp)
        val detection = detector.inspect(event, rootInActiveWindow)
        logDiagnostic(event, detection)

        val now = SystemClock.elapsedRealtime()
        val decision = reelCounter.update(detection, now)
        logDecision("Instagram", decision)
        if (decision.incrementReason != null) {
            recordCount(decision.incrementReason)
        } else if (!detection.isReelsViewer) {
            store.updateDetectorStatus("Instagram open — outside the Reels viewer")
        } else if (decision.detail.startsWith("Reels viewer candidate")) {
            store.updateDetectorStatus("Reels viewer candidate — confirming identity")
        }
        reelCounter.nextCheckDelay(now)?.let { handler.postDelayed(reelFollowUp, it) }
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
        if (decision.incrementReason != null ||
                event?.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
                decision.detail.startsWith("rejected") || now - lastShortsDiagnosticAt >= 500L) {
            lastShortsDiagnosticAt = now
            Log.d(TAG, "t=$now event=${event?.let {
                AccessibilityEvent.eventTypeToString(it.eventType)
            } ?: "FOLLOW_UP"} viewer=${observation.viewer} blocked=${observation.blocked} " +
                "scroll=${observation.scroll} decision=${decision.detail} ${observation.summary}")
        }
        decision.incrementReason?.let(::recordCount)
        if (overlay == null) {
            handler.removeCallbacks(shortsFollowUp)
            shortsCounter.nextCheckDelay(now)?.let { handler.postDelayed(shortsFollowUp, it) }
        }
    }

    private fun flushReels() {
        val now = SystemClock.elapsedRealtime()
        val decision = reelCounter.flush(now)
        logDecision("Instagram follow-up", decision)
        decision.incrementReason?.let(::recordCount)
        reelCounter.nextCheckDelay(now)?.let { handler.postDelayed(reelFollowUp, it) }
    }

    private fun logDecision(source: String, decision: ReelDecision) {
        if (decision.incrementReason != null || decision.detail.startsWith("rejected")) {
            Log.d(TAG, "$source decision=${decision.incrementReason ?: decision.detail}")
        }
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
        handler.removeCallbacks(reelFollowUp)
        shortsCounter.reset()
        reelCounter.reset()
        lastPackage = null
    }

    override fun onInterrupt() {
        handler.removeCallbacks(shortsFollowUp)
        handler.removeCallbacks(reelFollowUp)
        shortsCounter.suspend()
        Log.w(TAG, "Accessibility service interrupted")
        store.updateDetectorStatus("Accessibility interrupted — reopen FINITE if counting stops")
    }

    override fun onDestroy() {
        handler.removeCallbacks(shortsFollowUp)
        handler.removeCallbacks(reelFollowUp)
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
        private const val DIAGNOSTIC_THROTTLE_MS = 1_500L
    }
}

