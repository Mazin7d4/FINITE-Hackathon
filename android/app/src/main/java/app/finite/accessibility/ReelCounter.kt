package app.finite.accessibility

import kotlin.math.abs

internal data class ReelScroll(
    val eventTime: Long,
    val y: Int = 0,
    val deltaY: Int? = null,
    val deltaX: Int? = null,
)

/** Counts the entry and each completed vertical pager gesture in Instagram Reels. */
internal class ReelCounter {
    private var inViewer = false
    private var entryCounted = false
    private var outsideSince: Long? = null
    private var lastFingerprint: String? = null
    private var lastEventTime = -1L
    private var lastScrollEventTime: Long? = null
    private var lastCountAt: Long? = null
    private var lastSwipeCountAt: Long? = null
    private var pending: Gesture? = null

    private data class Gesture(
        var scroll: ReelScroll,
        var lastAt: Long,
        var maxMovement: Long,
        var fingerprint: String? = null,
        val suppressDuplicate: Boolean,
        var lastEventTime: Long,
    )

    fun reset() {
        inViewer = false
        entryCounted = false
        outsideSince = null
        lastFingerprint = null
        lastEventTime = -1L
        lastScrollEventTime = null
        lastCountAt = null
        lastSwipeCountAt = null
        pending = null
    }

    fun platformSuspend(now: Long) {
        if (inViewer && outsideSince == null) outsideSince = now
    }

    fun update(detection: Detection, now: Long): ReelDecision {
        var settledDecision: ReelDecision? = null
        pending?.let {
            val quiet = detection.scroll?.let { scroll ->
                scroll.eventTime - it.lastEventTime >= SETTLE_MS
            } ?: (now - it.lastAt >= SETTLE_MS)
            if (quiet) settledDecision = commitPending(it, now)
        }
        fun result(decision: ReelDecision): ReelDecision = settledDecision ?: decision
        if (!detection.isReelsViewer) {
            if (outsideSince == null) outsideSince = now
            if (inViewer && now - outsideSince!! >= EXIT_MS) {
                inViewer = false
                entryCounted = false
                pending = null
            }
            return result(ReelDecision(detail = "not in Reels viewer"))
        }
        outsideSince = null
        if (!inViewer) {
            inViewer = true
            lastFingerprint = detection.fingerprint
            if (detection.strongEvidence) {
                entryCounted = true
                lastCountAt = now
                return result(ReelDecision("Instagram viewer opened", "first Reel counted"))
            }
            entryCounted = false
            return result(ReelDecision(detail = "Reels viewer candidate — waiting for strong evidence"))
        }
        val fingerprint = detection.fingerprint
        detection.scroll?.let { scroll ->
            if (scroll.eventTime <= lastEventTime) {
                return result(ReelDecision(detail = "rejected duplicate/stale scroll"))
            }
            lastEventTime = scroll.eventTime
            lastScrollEventTime = scroll.eventTime
            if (abs((scroll.deltaX ?: 0).toLong()) > abs((scroll.deltaY ?: 0).toLong())) {
                return result(ReelDecision(detail = "rejected horizontal scroll"))
            }
            val gesture = pending
            if (gesture == null) {
                pending = Gesture(scroll, now, eventMovement(scroll),
                    fingerprint, settledDecision == null, scroll.eventTime)
            } else {
                gesture.maxMovement = maxOf(gesture.maxMovement, eventMovement(scroll))
                gesture.scroll = scroll
                gesture.lastAt = now
                gesture.lastEventTime = scroll.eventTime
                if (fingerprint != null) gesture.fingerprint = fingerprint
            }
            return result(ReelDecision(detail = "Instagram scroll burst pending"))
        }
        if (detection.strongFingerprint && fingerprint != null && fingerprint != lastFingerprint) {
            if (pending != null) {
                pending = null
                lastFingerprint = fingerprint
                lastCountAt = now
                lastSwipeCountAt = now
                return result(ReelDecision("Instagram identity-change", "fingerprint changed during scroll burst"))
            }
            if (lastSwipeCountAt?.let { countEventTime ->
                    lastScrollEventTime?.let { scrollEventTime ->
                        scrollEventTime - countEventTime in 0..DEDUPE_WINDOW_MS
                    }
                } == true
            ) {
                lastFingerprint = fingerprint
                return result(ReelDecision(detail = "rejected duplicate identity within swipe window"))
            }
            lastFingerprint = fingerprint
            lastCountAt = now
            return result(ReelDecision("Instagram identity-change", "fingerprint changed"))
        }
        return result(ReelDecision(detail = "no Reel transition signal"))
    }

    fun nextCheckDelay(now: Long): Long? = pending?.let {
        (it.lastAt + SETTLE_MS - now).coerceAtLeast(1L)
    } ?: if (inViewer) outsideSince?.let { (it + EXIT_MS - now).coerceAtLeast(1L) } else null

    fun flush(now: Long): ReelDecision {
        val gesture = pending ?: return ReelDecision(detail = "no pending Reel swipe")
        return if (now - gesture.lastAt >= SETTLE_MS) commitPending(gesture, now)
        else ReelDecision(detail = "Instagram scroll burst still settling")
    }

    private fun commitPending(gesture: Gesture, now: Long): ReelDecision {
        pending = null
        if (gesture.maxMovement < MIN_MOVEMENT) {
            return ReelDecision(detail = "rejected layout scroll, no movement")
        }
        if (gesture.suppressDuplicate &&
            lastSwipeCountAt?.let { now - it <= DEDUPE_WINDOW_MS } == true
        ) {
            return ReelDecision(detail = "rejected duplicate Reel scroll within swipe window")
        }
        gesture.fingerprint?.let { lastFingerprint = it }
        lastCountAt = now
        lastSwipeCountAt = gesture.lastEventTime
        return ReelDecision("Instagram scroll-burst", "settled vertical Reel pager swipe")
    }

    private fun eventMovement(scroll: ReelScroll): Long {
        val deltaMovement = abs(scroll.deltaY?.toLong() ?: 0L)
        val positionMovement = if (scroll.deltaY == null) abs(scroll.y.toLong()) else 0L
        return maxOf(deltaMovement, positionMovement)
    }

    companion object {
        internal const val EXIT_MS = 2_000L
        internal const val SETTLE_MS = 300L
        internal const val DEDUPE_WINDOW_MS = 600L
        private const val MIN_MOVEMENT = 8L
    }
}

internal data class ReelDecision(val incrementReason: String? = null, val detail: String)
