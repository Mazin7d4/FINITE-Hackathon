package app.finite.accessibility

import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

/** IDs observed on YouTube 21.43.523; classification remains best effort across versions. */
internal class ShortsDetector {
    fun inspect(event: AccessibilityEvent?, root: AccessibilityNodeInfo?): ShortsObservation {
        if (root?.packageName?.toString() != YOUTUBE_PACKAGE) {
            return ShortsObservation(ShortsViewer.UNKNOWN, summary = "YouTube window unavailable")
        }
        val ids = linkedSetOf<String>()
        // Stable identity candidates: author name and Short title/description only.
        // Counts, progress bars, and generic action labels (like/comment/share) are excluded.
        val identityParts = mutableListOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < 350) {
            val node = queue.removeFirst()
            visited += 1
            if (node.isVisibleToUser) {
                val id = resourceId(node.viewIdResourceName)
                if (id != null) {
                    ids.add(id)
                    if (id in IDENTITY_IDS) {
                        // Prefer contentDescription (accessibility-announced) then text.
                        val candidate = node.contentDescription?.toString()?.trim()
                            ?.takeIf { it.isValidIdentity() }
                            ?: node.text?.toString()?.trim()?.takeIf { it.isValidIdentity() }
                        candidate?.let { identityParts.add(it.take(120)) }
                    }
                }
            }
            for (index in 0 until node.childCount) {
                if (queue.size + visited >= 350) break
                node.getChild(index)?.let(queue::addLast)
            }
        }
        val engagementPanelOpen = ENGAGEMENT_PANEL_IDS.any(ids::contains)
        // Loading and end-page nodes can coexist with the pager's real scroll events while the
        // next Short is settling. Only an engagement sheet covers the content and should cancel
        // scroll parsing/pending gestures.
        val blocked = engagementPanelOpen
        val viewer = when {
            "watch_player" in ids -> ShortsViewer.OUTSIDE
            VIEWER_IDS.all(ids::contains) -> ShortsViewer.CONFIRMED
            VIEWER_IDS.any(ids::contains) -> ShortsViewer.UNKNOWN
            OUTSIDE_IDS.any(ids::contains) -> ShortsViewer.OUTSIDE
            else -> ShortsViewer.UNKNOWN
        }
        val fingerprint = if (viewer == ShortsViewer.CONFIRMED && identityParts.isNotEmpty()) {
            identityParts.joinToString("|")
                .hashCode().toUInt().toString(16)
        } else null
        val sourceId = resourceId(event?.source?.viewIdResourceName)
        val scroll = if (!blocked && viewer == ShortsViewer.CONFIRMED &&
            event?.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && sourceId in SCROLL_IDS
        ) {
            // The observed underlay FrameLayout reports sourceScrollable=false.
            ShortsScroll(
                eventTime = event.eventTime,
                source = sourceId!!,
                y = event.scrollY,
                deltaY = if (Build.VERSION.SDK_INT >= 28) event.scrollDeltaY.takeUnless { it == -1 } else null,
                deltaX = if (Build.VERSION.SDK_INT >= 28) event.scrollDeltaX.takeUnless { it == -1 } else null,
                pageIndex = event.fromIndex.takeIf {
                    it >= 0 && it == event.toIndex && event.itemCount > 1
                },
                relativeOffset = sourceId == "reel_player_underlay",
            )
        } else null
        return ShortsObservation(viewer, blocked, scroll,
            "signature=${VIEWER_IDS.count(ids::contains)}/3 source=$sourceId fp=${fingerprint ?: "none"} nodes=$visited",
            fingerprint, engagementPanelOpen)
    }

    private fun resourceId(value: String?): String? =
        value?.takeIf { RESOURCE_ID.matches(it) }?.substringAfter(":id/")

    /**
     * Returns true if the string looks like stable per-Short content (author name or title),
     * not a transient engagement value (pure number, like count with K/M suffix) or a
     * generic UI label.
     */
    private fun String.isValidIdentity(): Boolean {
        if (length !in 1..120) return false
        // Reject purely numeric strings and engagement-count patterns like "12.3K likes"
        if (all { it.isDigit() || it.isWhitespace() || it in ".,KkMmBb" }) return false
        val lower = lowercase()
        if (lower in GENERIC_LABELS) return false
        // Reject strings that are just an engagement count with a label
        if (ENGAGEMENT_PATTERN.matches(lower)) return false
        return true
    }

    companion object {
        private const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        private val VIEWER_IDS = setOf("reel_watch_fragment_root", "reel_watch_player", "reel_recycler")
        private val SCROLL_IDS = setOf("reel_player_underlay", "reel_recycler")
        private val ENGAGEMENT_PANEL_IDS = setOf("app_engagement_panel", "engagement_panel",
            "engagement_panel_content")
        private val OUTSIDE_IDS = setOf("results", "results_recycler_view", "search_box",
            "search_edit_text", "browse_fragment_layout_coordinator_layout")
        private val RESOURCE_ID = Regex("com\\.google\\.android\\.youtube:id/[A-Za-z0-9_]{1,100}")
        // Resource IDs that carry stable per-Short identity: uploader name and Short title.
        // Excludes like/comment/share counts, progress, timestamps, and generic navigation.
        internal val IDENTITY_IDS = setOf(
            "reel_player_header_text",   // uploader/channel name
            "channel_name",              // channel name fallback
            "reel_title",                // Short title overlay (when present)
            "reel_description",          // Short description/caption overlay
        )
        private val GENERIC_LABELS = setOf(
            "like", "comment", "share", "subscribe", "follow", "more", "mute", "unmute",
            "pause", "play", "home", "search", "shorts", "subscriptions", "library",
        )
        // Matches engagement counts like "1.2k likes", "345 comments", "share"
        private val ENGAGEMENT_PATTERN = Regex(
            """^\d[\d.,]*\s*[kmb]?\s*(like|likes|comment|comments|share|shares|view|views)?$"""
        )
    }
}
