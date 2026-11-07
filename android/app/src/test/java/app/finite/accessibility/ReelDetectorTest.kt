package app.finite.accessibility

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class ReelDetectorTest {
    private val detector = ReelDetector()

    @Test fun strongViewerIdIdentifiesViewer() {
        val root = node("com.instagram.android:id/clips_viewer_view_pager")
        val event = mock(AccessibilityEvent::class.java)
        `when`(event.source).thenReturn(root)

        val detection = detector.inspect(event, root)
        assertTrue(detection.isReelsViewer)
        assertTrue(detection.strongEvidence)
    }

    @Test fun combinedEvidenceWithActionsIdentifiesViewer() {
        val root = node("com.instagram.android:id/root")
        val label = node("com.instagram.android:id/label", text = "reels")
        val like = node("com.instagram.android:id/btn1", text = "like")
        val comment = node("com.instagram.android:id/btn2", text = "comment")
        val share = node("com.instagram.android:id/btn3", text = "share")

        setChildren(root, listOf(label, like, comment, share))
        val event = mock(AccessibilityEvent::class.java)

        val detection = detector.inspect(event, root)
        assertTrue(detection.isReelsViewer)
        assertFalse(detection.strongEvidence)
        assertEquals(3, detection.actionScore)
    }

    @Test fun identityFallbackUsesGenericTextWhenAuthorAndCaptionAbsent() {
        val root = node("com.instagram.android:id/clips_viewer_view_pager")
        val genericText = node("com.instagram.android:id/random_view", text = "Creator Showcase Video Title")
        setChildren(root, listOf(genericText))

        val event = mock(AccessibilityEvent::class.java)
        val detection = detector.inspect(event, root)

        assertTrue(detection.isReelsViewer)
        assertNotNull("fallback identity must produce a fingerprint", detection.fingerprint)
    }

    @Test fun fallbackFingerprintIsStableAcrossIdenticalHierarchies() {
        val event = mock(AccessibilityEvent::class.java)

        val root1 = node("com.instagram.android:id/clips_viewer_view_pager")
        val generic1 = node("com.instagram.android:id/text_view", text = "Night sky timelapse")
        setChildren(root1, listOf(generic1))

        val root2 = node("com.instagram.android:id/clips_viewer_view_pager")
        val generic2 = node("com.instagram.android:id/text_view", text = "Night sky timelapse")
        setChildren(root2, listOf(generic2))

        val fp1 = detector.inspect(event, root1).fingerprint
        val fp2 = detector.inspect(event, root2).fingerprint
        assertNotNull(fp1)
        assertEquals("identical fallback text must yield identical fingerprint", fp1, fp2)
    }

    @Test fun fallbackFingerprintDiffersForDifferentGenericText() {
        val event = mock(AccessibilityEvent::class.java)

        val root1 = node("com.instagram.android:id/clips_viewer_view_pager")
        val generic1 = node("com.instagram.android:id/text_view", text = "Morning coffee routine")
        setChildren(root1, listOf(generic1))

        val root2 = node("com.instagram.android:id/clips_viewer_view_pager")
        val generic2 = node("com.instagram.android:id/text_view", text = "Evening workout session")
        setChildren(root2, listOf(generic2))

        val fp1 = detector.inspect(event, root1).fingerprint
        val fp2 = detector.inspect(event, root2).fingerprint
        assertNotNull(fp1)
        assertNotNull(fp2)
        assertNotEquals("different fallback text must yield different fingerprints", fp1, fp2)
    }

    @Test fun fallbackIdentityIsNotUsedOutsideViewer() {
        // A hierarchy with no strong ID and insufficient action evidence is NOT a viewer.
        val root = node("com.instagram.android:id/home_feed")
        val text = node("com.instagram.android:id/post_title", text = "Interesting post content here")
        setChildren(root, listOf(text))

        val event = mock(AccessibilityEvent::class.java)
        val detection = detector.inspect(event, root)

        assertFalse("non-viewer hierarchy must not be classified as viewer", detection.isReelsViewer)
        assertNull("fallback identity must be absent outside the viewer", detection.fingerprint)
    }

    @Test fun identicalHierarchyProducesIdenticalFingerprint() {
        val root1 = node("com.instagram.android:id/clips_viewer_view_pager")
        val author1 = node("com.instagram.android:id/username", text = "photographer_daily")
        val caption1 = node("com.instagram.android:id/caption", text = "Sunset at the beach")
        setChildren(root1, listOf(author1, caption1))

        val root2 = node("com.instagram.android:id/clips_viewer_view_pager")
        val author2 = node("com.instagram.android:id/username", text = "photographer_daily")
        val caption2 = node("com.instagram.android:id/caption", text = "Sunset at the beach")
        setChildren(root2, listOf(author2, caption2))

        val event = mock(AccessibilityEvent::class.java)
        val detection1 = detector.inspect(event, root1)
        val detection2 = detector.inspect(event, root2)

        assertNotNull(detection1.fingerprint)
        assertEquals(detection1.fingerprint, detection2.fingerprint)
    }

    @Test fun differentContentProducesDifferentFingerprints() {
        val root1 = node("com.instagram.android:id/clips_viewer_view_pager")
        val author1 = node("com.instagram.android:id/username", text = "nature_daily")
        setChildren(root1, listOf(author1))

        val root2 = node("com.instagram.android:id/clips_viewer_view_pager")
        val author2 = node("com.instagram.android:id/username", text = "cooking_daily")
        setChildren(root2, listOf(author2))

        val event = mock(AccessibilityEvent::class.java)
        val detection1 = detector.inspect(event, root1)
        val detection2 = detector.inspect(event, root2)

        assertNotNull(detection1.fingerprint)
        assertNotNull(detection2.fingerprint)
        assertNotEquals(detection1.fingerprint, detection2.fingerprint)
    }

    private fun node(
        id: String,
        text: String? = null,
        description: String? = null,
    ): AccessibilityNodeInfo {
        val node = mock(AccessibilityNodeInfo::class.java)
        `when`(node.viewIdResourceName).thenReturn(id)
        if (text != null) `when`(node.text).thenReturn(text)
        if (description != null) `when`(node.contentDescription).thenReturn(description)
        `when`(node.childCount).thenReturn(0)
        return node
    }

    private fun setChildren(parent: AccessibilityNodeInfo, children: List<AccessibilityNodeInfo>) {
        `when`(parent.childCount).thenReturn(children.size)
        children.forEachIndexed { index, child ->
            `when`(parent.getChild(index)).thenReturn(child)
        }
    }
}
