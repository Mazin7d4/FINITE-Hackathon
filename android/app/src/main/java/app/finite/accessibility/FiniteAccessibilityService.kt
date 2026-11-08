package app.finite.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.core.content.ContextCompat
import app.finite.MainActivity
import app.finite.data.SessionStore
import app.finite.notifications.ReminderScheduler

class FiniteAccessibilityService : AccessibilityService() {
    private lateinit var store: SessionStore
    private lateinit var windowManager: WindowManager
    private val detector = ReelDetector()
    private val reelCounter = ReelCounter()
    private val shortsDetector = ShortsDetector()
    private val shortsCounter = ShortsCounter()
    private lateinit var reminders: ReminderScheduler
    private val handler = Handler(Looper.getMainLooper())
    private val scheduleGuard = object : Runnable {
        override fun run() {
            dismissOverlayIfPolicyChanged()
            handler.postDelayed(this, SCHEDULE_GUARD_INTERVAL_MS)
        }
    }
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
            } else if (intent?.action == SessionStore.ACTION_STATE_CHANGED) {
                dismissOverlayIfPolicyChanged()
            }
        }
    }

    private var overlay: View? = null
    private var lastDiagnosticAt = 0L

    override fun onCreate() {
        super.onCreate()
        store = SessionStore(this)
        reminders = ReminderScheduler(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        ContextCompat.registerReceiver(this, lifecycleReceiver,
            IntentFilter(ACTION_RESET_DETECTION).apply { addAction(SessionStore.ACTION_STATE_CHANGED) },
            ContextCompat.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
        handler.post(scheduleGuard)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility service connected")
        store.updateDetectorStatus("Accessibility ready")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString()
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            dismissOverlayIfPolicyChanged(packageName ?: rootInActiveWindow?.packageName?.toString())
        }
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
        val packageName = lastPackage
        val result = if (packageName == INSTAGRAM_PACKAGE) {
            store.increment(SessionStore.INSTAGRAM, reason)
        } else if (packageName == YOUTUBE_PACKAGE) {
            store.increment(SessionStore.YOUTUBE, reason)
        } else {
            Log.w(TAG, "Ignoring count without a supported foreground package")
            return
        }
        Log.i(TAG, "Count=${result.state.count}/${result.state.limit}; reason=$reason")
        if (result.shouldShowLimit && overlay == null && packageName != null && store.canShowLimitFor(packageName)) {
            showProgressOverlay(result.state.count, result.state.limit, isLimit = true)
        } else if (overlay == null && packageName != null && store.canShowLimitFor(packageName)) {
            reminders.claimDueReminder(result.state.count, result.state.limit)?.let {
                showProgressOverlay(result.state.count, result.state.limit, isLimit = false)
            }
        }
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

    private fun showProgressOverlay(count: Int, limit: Int, isLimit: Boolean) {
        val remaining = (limit - count).coerceAtLeast(0)
        val root = FrameLayout(this).apply {
            setPadding(dp(20), dp(28), dp(20), dp(28))
            setBackgroundColor(Color.argb(168, 7, 19, 27))
            alpha = 0f
        }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(26), dp(24), dp(26), dp(22))
            background = GradientDrawable().apply {
                orientation = GradientDrawable.Orientation.TOP_BOTTOM
                colors = intArrayOf(
                    Color.parseColor("#FCFEFD"),
                    Color.parseColor("#F3F8F6"),
                    Color.parseColor("#E7F1EE"),
                )
                cornerRadius = dp(30).toFloat()
                setStroke(dp(1), Color.parseColor("#AFC7C8"))
            }
            elevation = dp(24).toFloat()
            alpha = 0f
            scaleX = 0.92f
            scaleY = 0.92f
            translationY = dp(24).toFloat()
        }

        panel.addView(View(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor(if (isLimit) FINITE_GOLD else FINITE_PRIMARY))
                cornerRadius = dp(99).toFloat()
            }
        }, LinearLayout.LayoutParams(dp(38), dp(4)).apply { bottomMargin = dp(18) })

        panel.addView(TextView(this).apply {
            text = if (isLimit) "✓" else count.toString()
            setTextColor(Color.parseColor(if (isLimit) FINITE_GOLD_DARK else FINITE_PRIMARY_DARK))
            textSize = if (isLimit) 28f else 25f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor(if (isLimit) "#F7EED8" else "#DCECEA"))
                shape = GradientDrawable.OVAL
                setStroke(dp(1), Color.parseColor(if (isLimit) "#E7D3A3" else "#B7D3D0"))
            }
        }, LinearLayout.LayoutParams(dp(64), dp(64)).apply { bottomMargin = dp(17) })

        panel.addView(TextView(this).apply {
            text = if (isLimit) "DAILY BOUNDARY REACHED" else "MINDFUL CHECK-IN"
            setTextColor(Color.parseColor(FINITE_SECONDARY_TEXT))
            textSize = 11f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            letterSpacing = 0.14f
            gravity = Gravity.CENTER
        }, matchWrap(top = 0, bottom = 8))

        panel.addView(TextView(this).apply {
            text = if (isLimit) "That’s your $limit for today." else "You’re $count videos in."
            setTextColor(Color.parseColor(FINITE_TEXT))
            textSize = 25f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = -0.02f
        }, matchWrap(top = 0, bottom = 7))

        panel.addView(TextView(this).apply {
            text = if (isLimit) {
                "You kept the promise you made to yourself. Take the win and leave the feed behind."
            } else {
                "$remaining ${if (remaining == 1) "video" else "videos"} left in today’s plan. Pause for a second, then choose what feels right."
            }
            setTextColor(Color.parseColor(FINITE_SECONDARY_TEXT))
            textSize = 14f
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.18f)
        }, matchWrap(top = 0, bottom = 22))

        val stats = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#DDEAE7"))
                cornerRadius = dp(18).toFloat()
            }
            setPadding(dp(8), dp(12), dp(8), dp(12))
            addView(statColumn(count.toString(), "WATCHED"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(View(this@FiniteAccessibilityService).apply { setBackgroundColor(Color.parseColor("#B8CECA")) }, LinearLayout.LayoutParams(dp(1), dp(34)))
            addView(statColumn(remaining.toString(), "LEFT TODAY"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        panel.addView(stats, matchWrap(top = 0, bottom = 14))

        val progress = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#D4E1DF"))
                cornerRadius = dp(99).toFloat()
            }
            clipToOutline = true
            addView(View(this@FiniteAccessibilityService).apply {
                background = GradientDrawable().apply {
                    orientation = GradientDrawable.Orientation.LEFT_RIGHT
                    colors = if (isLimit) intArrayOf(Color.parseColor(FINITE_GOLD), Color.parseColor("#D69A58"))
                        else intArrayOf(Color.parseColor("#6B9299"), Color.parseColor("#73A99B"))
                    cornerRadius = dp(99).toFloat()
                }
            }, LinearLayout.LayoutParams(0, dp(8), count.coerceAtMost(limit).toFloat()))
            addView(Space(this@FiniteAccessibilityService), LinearLayout.LayoutParams(0, dp(8), remaining.coerceAtLeast(0).toFloat()))
        }
        panel.addView(progress, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8)).apply { bottomMargin = dp(22) })

        if (isLimit) {
            panel.addView(overlayButton("I’m done for today", primary = true) {
                dismissOverlayAnimated {
                    store.end("Goal complete — session ended")
                    openFinite()
                }
            }, matchWrap(top = 0, bottom = 10))
            panel.addView(overlayButton("Continue anyway", primary = false) {
                dismissOverlayAnimated { store.acknowledgeLimit() }
            }, matchWrap(top = 0, bottom = 0))
        } else {
            panel.addView(overlayButton("Continue with intention", primary = true) {
                dismissOverlayAnimated()
            }, matchWrap(top = 0, bottom = 10))
            panel.addView(overlayButton("End my session", primary = false) {
                dismissOverlayAnimated {
                    store.end("Session ended at mindful reminder")
                    openFinite()
                }
            }, matchWrap(top = 0, bottom = 0))
        }

        panel.addView(TextView(this).apply {
            text = "FINITE  •  YOUR ATTENTION, ON PURPOSE"
            setTextColor(Color.parseColor("#78908F"))
            textSize = 9f
            letterSpacing = 0.1f
            gravity = Gravity.CENTER
        }, matchWrap(top = 18, bottom = 0))

        root.addView(panel, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER,
        ))

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.CENTER }

        try {
            windowManager.addView(root, params)
            overlay = root
            root.animate().alpha(1f).setDuration(220L).setInterpolator(DecelerateInterpolator()).start()
            panel.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                .setDuration(420L).setInterpolator(OvershootInterpolator(0.72f)).start()
            Log.i(TAG, if (isLimit) "Limit overlay shown" else "Video reminder overlay shown")
        } catch (error: RuntimeException) {
            Log.e(TAG, "Unable to show accessibility overlay", error)
            store.updateDetectorStatus("FINITE popup failed — see app logs")
        }
    }

    private fun statColumn(value: String, label: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        addView(TextView(this@FiniteAccessibilityService).apply {
            text = value
            setTextColor(Color.parseColor(FINITE_TEXT))
            textSize = 22f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
        })
        addView(TextView(this@FiniteAccessibilityService).apply {
            text = label
            setTextColor(Color.parseColor(FINITE_SECONDARY_TEXT))
            textSize = 9f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            letterSpacing = 0.12f
            gravity = Gravity.CENTER
        })
    }

    private fun overlayButton(label: String, primary: Boolean, action: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(if (primary) FINITE_SURFACE else FINITE_TEXT))
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            minHeight = dp(50)
            setPadding(dp(16), 0, dp(16), 0)
            val shape = GradientDrawable().apply {
                setColor(Color.parseColor(if (primary) FINITE_PRIMARY else FINITE_SURFACE))
                cornerRadius = dp(16).toFloat()
                if (!primary) setStroke(dp(1), Color.parseColor("#C8D8D5"))
            }
            background = RippleDrawable(ColorStateList.valueOf(Color.parseColor("#2AFFFFFF")), shape, null)
            isClickable = true
            setOnClickListener { action() }
        }

    private fun openFinite() {
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }

    private fun dismissOverlayAnimated(after: () -> Unit = {}) {
        val view = overlay ?: run { after(); return }
        view.animate().alpha(0f).translationY(dp(8).toFloat()).setDuration(170L)
            .setInterpolator(DecelerateInterpolator()).withEndAction {
                try { windowManager.removeView(view) } catch (_: RuntimeException) {}
                if (overlay === view) overlay = null
                after()
            }.start()
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

    private fun dismissOverlayIfPolicyChanged(currentPackage: String? = lastPackage) {
        if (overlay == null) return
        val packageName = lastPackage
        val switchedToAnotherPackage = currentPackage != null &&
            currentPackage != packageName && currentPackage != applicationContext.packageName
        if (!store.snapshot().active || packageName == null || switchedToAnotherPackage ||
            !store.canShowLimitFor(packageName)) {
            dismissOverlay()
            Log.d(TAG, "limit overlay dismissed; session policy no longer applies")
        }
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
        handler.removeCallbacks(scheduleGuard)
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
        private const val INSTAGRAM_PACKAGE = SessionStore.INSTAGRAM_PACKAGE
        private const val YOUTUBE_PACKAGE = SessionStore.YOUTUBE_PACKAGE
        private const val DIAGNOSTIC_THROTTLE_MS = 1_500L
        private const val SCHEDULE_GUARD_INTERVAL_MS = 60_000L

        // Keep the native accessibility overlay aligned with src/screens/theme.ts.
        private const val FINITE_PRIMARY = "#5B7A8C"
        private const val FINITE_PRIMARY_DARK = "#365D68"
        private const val FINITE_GOLD = "#C99A53"
        private const val FINITE_GOLD_DARK = "#8C642B"
        private const val FINITE_TEXT = "#162B39"
        private const val FINITE_SECONDARY_TEXT = "#607684"
        private const val FINITE_SURFACE = "#FFFFFF"
    }
}

