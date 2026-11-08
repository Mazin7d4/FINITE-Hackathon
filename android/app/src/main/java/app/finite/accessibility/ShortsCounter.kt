package app.finite.accessibility

import kotlin.math.abs

internal enum class ShortsViewer { CONFIRMED, UNKNOWN, OUTSIDE }

internal data class ShortsScroll(
    val eventTime: Long,
    val source: String,
    val y: Int = 0,
    val deltaY: Int? = null,
    val deltaX: Int? = null,
    val pageIndex: Int? = null,
    val relativeOffset: Boolean = false,
)

internal data class ShortsObservation(
    val viewer: ShortsViewer,
    val blocked: Boolean = false,
    val scroll: ShortsScroll? = null,
    val summary: String = "",
    val fingerprint: String? = null,
    val engagementPanelOpen: Boolean = false,
)

internal data class ShortsDecision(val incrementReason: String? = null, val detail: String)

/** Counts entry and completed vertical Shorts pager gestures; identity is only a fast dedupe aid. */
internal class ShortsCounter {
    private var entered = false
    private var outsideSince: Long? = null
    private var outsideHits = 0
    private var lastCountAt: Long? = null
    private var lastEventTime = -1L
    private var lastScrollEventTime: Long? = null
    private var lastFingerprint: String? = null
    private var lastSwipeCountAt: Long? = null
    private var pending: Gesture? = null
    private var excursion: Excursion? = null

    private data class Gesture(
        var scroll: ShortsScroll,
        var lastAt: Long,
        var maxMovement: Long,
        var fingerprint: String? = null,
        val suppressDuplicate: Boolean,
        var lastEventTime: Long,
    )

    private data class Excursion(
        var previousY: Int,
        var reachedLargeOffset: Boolean,
        var lastEventTime: Long,
        var countedByFallback: Boolean = false,
    )

    fun reset() {
        entered = false
        outsideSince = null
        outsideHits = 0
        lastCountAt = null
        lastEventTime = -1L
        lastScrollEventTime = null
        lastFingerprint = null
        lastSwipeCountAt = null
        pending = null
        excursion = null
    }

    fun suspend() {
        pending = null
        excursion = null
        outsideSince = null
        outsideHits = 0
    }

    fun update(observation: ShortsObservation, now: Long): ShortsDecision {
        var settledDecision: ShortsDecision? = null
        pending?.let { gesture ->
            val quiet = if (observation.scroll?.relativeOffset == true && excursion != null) {
                false
            } else if (observation.scroll == null && excursion != null) {
                now - gesture.lastAt >= RELATIVE_SETTLE_MS
            } else {
                observation.scroll?.let { scroll ->
                    scroll.eventTime - gesture.lastEventTime >= SETTLE_MS
                } ?: (now - gesture.lastAt >= SETTLE_MS)
            }
            if (quiet) settledDecision = commitPending(gesture, now)
        }
        var immediateDecision: ShortsDecision? = null
        fun result(decision: ShortsDecision): ShortsDecision =
            settledDecision ?: immediateDecision ?: decision

        if (observation.engagementPanelOpen) {
            pending = null
            excursion = null
            return result(ShortsDecision(detail = "rejected swipe: engagement panel open"))
        }
        if (observation.viewer == ShortsViewer.OUTSIDE) {
            if (outsideSince == null) outsideSince = now
            outsideHits += 1
            if (entered && outsideHits >= 2 && now - outsideSince!! >= EXIT_MS) {
                entered = false
                lastFingerprint = null
                pending = null
                excursion = null
            }
            return result(ShortsDecision(detail = "outside Shorts viewer"))
        }
        if (observation.viewer == ShortsViewer.UNKNOWN) {
            return result(ShortsDecision(detail = "transient Shorts viewer frame"))
        }

        outsideSince = null
        outsideHits = 0
        if (!entered) {
            entered = true
            lastCountAt = now
            lastFingerprint = observation.fingerprint
            return result(ShortsDecision("YouTube viewer opened", "first Short counted"))
        }

        val fingerprint = observation.fingerprint
        observation.scroll?.let { scroll ->
            if (scroll.eventTime <= lastEventTime) {
                return result(ShortsDecision(detail = "rejected duplicate/stale scroll"))
            }
            lastEventTime = scroll.eventTime
            lastScrollEventTime = scroll.eventTime
            if (abs((scroll.deltaX ?: 0).toLong()) > abs((scroll.deltaY ?: 0).toLong())) {
                return result(ShortsDecision(detail = "rejected horizontal scroll"))
            }

            if (scroll.relativeOffset) {
                val currentY = scroll.y
                val active = excursion
                if (active != null && active.previousY <= EXCURSION_RETURN_Y &&
                    abs(currentY) >= EXCURSION_START_Y && active.reachedLargeOffset
                ) {
                    immediateDecision = if (active.countedByFallback) {
                        ShortsDecision(detail = "rejected: excursion already counted by quiet fallback")
                    } else {
                        commitExcursion(active, scroll.eventTime)
                    }
                    excursion = null
                    pending = null
                }
                if (excursion == null && abs(currentY) >= EXCURSION_START_Y) {
                    excursion = Excursion(currentY, true, scroll.eventTime)
                } else if (excursion != null) {
                    excursion!!.reachedLargeOffset = excursion!!.reachedLargeOffset ||
                        abs(currentY) >= EXCURSION_START_Y
                    excursion!!.previousY = currentY
                    excursion!!.lastEventTime = scroll.eventTime
                    if (abs(currentY) <= EXCURSION_RETURN_Y && excursion!!.reachedLargeOffset) {
                        immediateDecision = if (excursion!!.countedByFallback) {
                            ShortsDecision(detail = "rejected: excursion already counted by quiet fallback")
                        } else {
                            commitExcursion(excursion!!, scroll.eventTime)
                        }
                        excursion = null
                        pending = null
                    }
                }
                if (excursion != null) {
                    val gesture = pending
                    if (gesture == null) {
                        pending = Gesture(scroll, now, eventMovement(scroll), fingerprint,
                            settledDecision == null, scroll.eventTime)
                    } else {
                        gesture.maxMovement = maxOf(gesture.maxMovement, eventMovement(scroll))
                        gesture.scroll = scroll
                        gesture.lastAt = now
                        gesture.lastEventTime = scroll.eventTime
                        if (fingerprint != null) gesture.fingerprint = fingerprint
                    }
                }
                return result(ShortsDecision(detail = "Shorts relative page excursion pending"))
            }

            val gesture = pending
            if (gesture == null) {
                pending = Gesture(scroll, now, eventMovement(scroll), fingerprint,
                    settledDecision == null, scroll.eventTime)
            } else {
                gesture.maxMovement = maxOf(gesture.maxMovement, eventMovement(scroll))
                gesture.scroll = scroll
                gesture.lastAt = now
                gesture.lastEventTime = scroll.eventTime
                if (fingerprint != null) gesture.fingerprint = fingerprint
            }
            return result(ShortsDecision(detail = "Shorts scroll burst pending"))
        }

        if (fingerprint != null && fingerprint != lastFingerprint) {
            if (pending != null) {
                pending = null
                lastFingerprint = fingerprint
                lastCountAt = now
                lastSwipeCountAt = lastScrollEventTime ?: now
                return result(ShortsDecision("Shorts identity-change", "fingerprint changed during scroll burst"))
            }
            val duplicateWindow = lastSwipeCountAt?.let { countEventTime ->
                lastScrollEventTime?.let { scrollEventTime ->
                    scrollEventTime - countEventTime in 0..DEDUPE_WINDOW_MS
                }
            } == true
            if (duplicateWindow) {
                lastFingerprint = fingerprint
                return result(ShortsDecision(detail = "rejected duplicate identity within swipe window"))
            }
            lastFingerprint = fingerprint
            lastCountAt = now
            return result(ShortsDecision("Shorts identity-change", "fingerprint changed"))
        }
        return result(ShortsDecision(detail = "no Short transition signal"))
    }

    fun nextCheckDelay(now: Long): Long? = pending?.let {
        val settleMs = if (excursion != null) RELATIVE_SETTLE_MS else SETTLE_MS
        (it.lastAt + settleMs - now).coerceAtLeast(1L)
    } ?: if (entered) outsideSince?.let { (it + EXIT_MS - now).coerceAtLeast(1L) } else null

    private fun commitPending(gesture: Gesture, now: Long): ShortsDecision {
        pending = null
        if (gesture.maxMovement < MIN_MOVEMENT) {
            return ShortsDecision(detail = "rejected layout scroll, no movement")
        }
        if (gesture.suppressDuplicate && lastSwipeCountAt?.let {
                gesture.lastEventTime - it in 0..DEDUPE_WINDOW_MS
            } == true
        ) {
            return ShortsDecision(detail = "rejected duplicate Shorts scroll within swipe window")
        }
        excursion?.let { it.countedByFallback = true }
        gesture.fingerprint?.let { lastFingerprint = it }
        lastCountAt = now
        lastSwipeCountAt = gesture.lastEventTime
        return ShortsDecision("Shorts scroll-burst", "settled vertical Shorts pager swipe")
    }

    private fun commitExcursion(excursion: Excursion, eventTime: Long): ShortsDecision {
        lastCountAt = eventTime
        lastSwipeCountAt = eventTime
        lastScrollEventTime = eventTime
        return ShortsDecision("Shorts page-settled", "relative Shorts page returned to zero")
    }

    private fun eventMovement(scroll: ShortsScroll): Long {
        val deltaMovement = abs(scroll.deltaY?.toLong() ?: 0L)
        val positionMovement = if (scroll.relativeOffset || scroll.deltaY == null) {
            abs(scroll.y.toLong())
        } else 0L
        return maxOf(deltaMovement, positionMovement)
    }

    companion object {
        private const val EXIT_MS = 2_000L
        private const val SETTLE_MS = 300L
        private const val DEDUPE_WINDOW_MS = 600L
        private const val MIN_MOVEMENT = 8L
        private const val RELATIVE_SETTLE_MS = 1_000L
        private const val EXCURSION_START_Y = 100
        private const val EXCURSION_RETURN_Y = 2
    }
}
