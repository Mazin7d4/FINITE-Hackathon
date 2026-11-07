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
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < 350) {
            val node = queue.removeFirst()
            visited += 1
            if (node.isVisibleToUser) resourceId(node.viewIdResourceName)?.let(ids::add)
            for (index in 0 until node.childCount) {
                if (queue.size + visited >= 350) break
                node.getChild(index)?.let(queue::addLast)
            }
        }
        val blocked = BLOCKING_IDS.any(ids::contains)
        val viewer = when {
            "watch_player" in ids -> ShortsViewer.OUTSIDE
            VIEWER_IDS.all(ids::contains) -> ShortsViewer.CONFIRMED
            VIEWER_IDS.any(ids::contains) -> ShortsViewer.UNKNOWN
            OUTSIDE_IDS.any(ids::contains) -> ShortsViewer.OUTSIDE
            else -> ShortsViewer.UNKNOWN
        }
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
            "signature=${VIEWER_IDS.count(ids::contains)}/3 source=$sourceId nodes=$visited")
    }

    private fun resourceId(value: String?): String? =
        value?.takeIf { RESOURCE_ID.matches(it) }?.substringAfter(":id/")

    companion object {
        private const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        private val VIEWER_IDS = setOf("reel_watch_fragment_root", "reel_watch_player", "reel_recycler")
        private val SCROLL_IDS = setOf("reel_player_underlay", "reel_recycler")
        private val BLOCKING_IDS = setOf("app_engagement_panel", "engagement_panel",
            "engagement_panel_content", "reel_playback_loading_spinner", "reel_player_end_page_container")
        private val OUTSIDE_IDS = setOf("results", "results_recycler_view", "search_box",
            "search_edit_text", "browse_fragment_layout_coordinator_layout")
        private val RESOURCE_ID = Regex("com\\.google\\.android\\.youtube:id/[A-Za-z0-9_]{1,100}")
    }
}
