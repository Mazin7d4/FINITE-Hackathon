package app.finite.accessibility

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class ShortsDetectorTest {
    private val detector = ShortsDetector()

    @Test fun allThreeObservedVisibleIdsAreRequired() {
        assertEquals(ShortsViewer.CONFIRMED, detector.inspect(null, root(viewerNodes())).viewer)
        assertNotEquals(ShortsViewer.CONFIRMED, detector.inspect(null, root(viewerNodes().take(2))).viewer)
        assertNotEquals(ShortsViewer.CONFIRMED,
            detector.inspect(null, root(listOf(node("reel_time_bar")))).viewer)
        val hidden = node("reel_recycler")
        `when`(hidden.isVisibleToUser).thenReturn(false)
        assertNotEquals(ShortsViewer.CONFIRMED,
            detector.inspect(null, root(viewerNodes().take(2) + hidden)).viewer)
    }

    @Test fun ordinaryPlaybackAndWrongWindowsCannotBeShorts() {
        assertEquals(ShortsViewer.OUTSIDE,
            detector.inspect(null, root(viewerNodes() + node("watch_player"))).viewer)
        val tree = root(viewerNodes())
        `when`(tree.packageName).thenReturn("com.instagram.android")
        assertEquals(ShortsViewer.UNKNOWN, detector.inspect(null, tree).viewer)
    }

    @Test fun underlayScrollWorksWithoutScrollableFlagOrItemIndices() {
        val event = event("reel_player_underlay")
        assertFalse(event.source!!.isScrollable)
        assertEquals(-1, event.fromIndex)
        val scroll = detector.inspect(event, root(viewerNodes())).scroll
        assertNotNull(scroll)
        assertEquals(-550, scroll!!.y)
        assertTrue(scroll.relativeOffset)
    }

    @Test fun genericScrollAndContentChangesAreNotTransitionSignals() {
        assertNull(detector.inspect(event("results"), root(viewerNodes())).scroll)
        val event = event("reel_recycler")
        `when`(event.eventType).thenReturn(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
        assertNull(detector.inspect(event, root(viewerNodes())).scroll)
    }

    @Test fun visiblePanelsLoadingAndEndPagesSuppressScrolls() {
        for (id in listOf("app_engagement_panel", "engagement_panel", "reel_playback_loading_spinner",
            "reel_player_end_page_container")) {
            val result = detector.inspect(event("reel_player_underlay"), root(viewerNodes() + node(id)))
            assertTrue(result.blocked)
            assertNull(result.scroll)
        }
    }

    @Test fun captionAndAccountTextAreNeverRead() {
        val caption = node("reel_player_footer_container")
        detector.inspect(null, root(viewerNodes() + caption))
        verify(caption, never()).text
        verify(caption, never()).contentDescription
    }

    @Test fun fingerprintPopulatedFromChannelNameInViewer() {
        val channel = nodeWithText("reel_player_header_text", "some_channel")
        val obs = detector.inspect(null, root(viewerNodes() + channel))
        assertEquals(ShortsViewer.CONFIRMED, obs.viewer)
        assertNotNull("fingerprint must be populated when channel name is visible", obs.fingerprint)
    }

    @Test fun fingerprintAbsentWhenNoIdentityNodesPresent() {
        val obs = detector.inspect(null, root(viewerNodes()))
        assertEquals(ShortsViewer.CONFIRMED, obs.viewer)
        assertNull("fingerprint must be null when no identity IDs present", obs.fingerprint)
    }

    @Test fun identicalIdentityNodesProduceIdenticalFingerprints() {
        val obs1 = detector.inspect(null, root(viewerNodes() + nodeWithText("reel_title", "My Short Title")))
        val obs2 = detector.inspect(null, root(viewerNodes() + nodeWithText("reel_title", "My Short Title")))
        assertNotNull(obs1.fingerprint)
        assertEquals("same visible title must produce same fingerprint", obs1.fingerprint, obs2.fingerprint)
    }

    @Test fun differentIdentityNodesPproduceDifferentFingerprints() {
        val obs1 = detector.inspect(null, root(viewerNodes() + nodeWithText("reel_player_header_text", "creator_alpha")))
        val obs2 = detector.inspect(null, root(viewerNodes() + nodeWithText("reel_player_header_text", "creator_beta")))
        assertNotNull(obs1.fingerprint)
        assertNotNull(obs2.fingerprint)
        assertNotEquals("different channel names must produce different fingerprints", obs1.fingerprint, obs2.fingerprint)
    }

    @Test fun engagementCountsAndGenericLabelsAreExcludedFromFingerprint() {
        // A node with a pure-numeric engagement count must not produce a fingerprint.
        val countNode = nodeWithText("reel_player_header_text", "1234")
        val obs = detector.inspect(null, root(viewerNodes() + countNode))
        assertNull("pure numeric count must not contribute to fingerprint", obs.fingerprint)
    }

    private fun viewerNodes() = listOf(node("reel_watch_fragment_root"),
        node("reel_watch_player"), node("reel_recycler"))

    private fun root(children: List<AccessibilityNodeInfo>): AccessibilityNodeInfo {
        val root = node("root")
        `when`(root.packageName).thenReturn("com.google.android.youtube")
        `when`(root.childCount).thenReturn(children.size)
        children.forEachIndexed { index, child -> `when`(root.getChild(index)).thenReturn(child) }
        return root
    }

    private fun node(id: String): AccessibilityNodeInfo {
        val node = mock(AccessibilityNodeInfo::class.java)
        `when`(node.viewIdResourceName).thenReturn("com.google.android.youtube:id/$id")
        `when`(node.isVisibleToUser).thenReturn(true)
        return node
    }

    private fun nodeWithText(id: String, text: String): AccessibilityNodeInfo {
        val node = node(id)
        `when`(node.text).thenReturn(text)
        return node
    }

    private fun event(sourceId: String): AccessibilityEvent {
        val event = mock(AccessibilityEvent::class.java)
        val source = node(sourceId)
        `when`(event.source).thenReturn(source)
        `when`(event.eventType).thenReturn(AccessibilityEvent.TYPE_VIEW_SCROLLED)
        `when`(event.eventTime).thenReturn(1_000L)
        `when`(event.scrollY).thenReturn(-550)
        `when`(event.fromIndex).thenReturn(-1)
        return event
    }
}
