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
)

internal data class ShortsDecision(val incrementReason: String? = null, val detail: String)

/** On-device, scroll-only counter. No captions, author identities, or engagement values. */
internal class ShortsCounter {
    private var entered = false
    private var outsideSince: Long? = null
    private var outsideHits = 0
    private var lastCountAt: Long? = null
    private var lastEventTime = -1L
    private var lastPage: Pair<String, Int>? = null
    private var lastOffset: Pair<String, Int>? = null
    private var pending: Gesture? = null

    private data class Gesture(
        var scroll: ShortsScroll,
        var lastAt: Long,
        var movement: Long,
        var measured: Boolean,
    )

    fun reset() {
        entered = false
        lastCountAt = null
        lastEventTime = -1L
        lastPage = null
        lastOffset = null
        suspend()
    }

    // Switching platforms cancels unfinished swipes, but preserves the viewer-entry latch.
    fun suspend() {
        pending = null
        outsideSince = null
        outsideHits = 0
    }

    fun update(observation: ShortsObservation, now: Long): ShortsDecision {
        if (observation.blocked) {
            suspend()
            return ShortsDecision(detail = "blocked panel/loading; pending swipe cancelled")
        }
        if (observation.viewer != ShortsViewer.CONFIRMED) {
            pending = null
            if (observation.viewer == ShortsViewer.OUTSIDE) {
                if (outsideSince == null) outsideSince = now
                outsideHits += 1
                if (outsideHits >= 2 && now - outsideSince!! >= EXIT_MS) entered = false
            } else {
                outsideSince = null
                outsideHits = 0
            }
            return ShortsDecision(detail = "viewer not confirmed; not counting")
        }
        outsideSince = null
        outsideHits = 0
        if (!entered) {
            entered = true
            lastCountAt = now
            lastEventTime = observation.scroll?.eventTime ?: -1L
            lastPage = observation.scroll?.let { scroll -> scroll.pageIndex?.let { scroll.source to it } }
            lastOffset = observation.scroll?.let { it.source to it.y }
            return ShortsDecision("YouTube Shorts viewer opened", "accepted first Short")
        }
        observation.scroll?.let { scroll ->
            if (scroll.eventTime <= lastEventTime) return ShortsDecision(detail = "duplicate/stale event")
            lastEventTime = scroll.eventTime
            if (lastCountAt?.let { now - it < DEBOUNCE_MS } == true) {
                return ShortsDecision(detail = "700 ms debounce")
            }
            if (abs((scroll.deltaX ?: 0).toLong()) > abs((scroll.deltaY ?: 0).toLong())) {
                return ShortsDecision(detail = "horizontal scroll")
            }
            val gesture = pending
            if (gesture == null) {
                val previous = lastOffset?.takeIf { it.first == scroll.source }?.second
                val movement = when {
                    scroll.relativeOffset && scroll.y != 0 -> scroll.y.toLong()
                    !scroll.relativeOffset && previous != null -> scroll.y.toLong() - previous
                    scroll.deltaY != null -> scroll.deltaY.toLong()
                    else -> scroll.y.toLong()
                }
                pending = Gesture(scroll, now, movement,
                    scroll.y != 0 || (scroll.deltaY ?: 0) != 0 || (previous ?: 0) != 0)
            } else {
                gesture.movement = when {
                    scroll.relativeOffset && (scroll.y != 0 || gesture.scroll.y != 0) -> scroll.y.toLong()
                    !scroll.relativeOffset && scroll.source == gesture.scroll.source ->
                        gesture.movement + scroll.y.toLong() - gesture.scroll.y
                    else -> gesture.movement + (scroll.deltaY ?: 0)
                }
                gesture.measured = gesture.measured || scroll.y != 0 || (scroll.deltaY ?: 0) != 0
                gesture.scroll = scroll
                gesture.lastAt = now
            }
            return ShortsDecision(detail = "valid Shorts scroll queued; waiting for quiet")
        }
        val gesture = pending ?: return ShortsDecision(detail = "no scroll; content changes ignored")
        if (now - gesture.lastAt < SETTLE_MS) return ShortsDecision(detail = "scroll still settling")
        pending = null
        val scroll = gesture.scroll
        val page = scroll.pageIndex?.let { scroll.source to it }
        if (page != null && page == lastPage) return ShortsDecision(detail = "same committed page")
        if (page == null && !scroll.relativeOffset && scroll.y != 0 &&
            lastOffset == (scroll.source to scroll.y)
        ) return ShortsDecision(detail = "unchanged cumulative scroll position")
        if (page == null && gesture.measured && abs(gesture.movement) < 8L) {
            return ShortsDecision(detail = "cancelled swipe or tiny movement")
        }
        lastPage = page
        lastOffset = scroll.source to scroll.y
        lastCountAt = now
        return ShortsDecision("YouTube Shorts transition", if (page != null) {
            "accepted committed page=${page.second}"
        } else "accepted settled scroll burst; best effort")
    }

    fun nextCheckDelay(now: Long): Long? {
        pending?.let { return (it.lastAt + SETTLE_MS - now).coerceAtLeast(1L) }
        if (entered) outsideSince?.let { return (it + EXIT_MS - now).coerceAtLeast(1L) }
        return null
    }

    companion object {
        private const val DEBOUNCE_MS = 700L
        private const val SETTLE_MS = 200L
        private const val EXIT_MS = 600L
    }
}
