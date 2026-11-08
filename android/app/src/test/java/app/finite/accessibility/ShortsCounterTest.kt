package app.finite.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ShortsCounterTest {
    private fun viewer(fp: String? = null, scroll: ShortsScroll? = null,
                       blocked: Boolean = false, unknown: Boolean = false) = ShortsObservation(
        if (unknown) ShortsViewer.UNKNOWN else ShortsViewer.CONFIRMED,
        blocked = blocked, scroll = scroll, fingerprint = fp,
    )
    private fun swipe(time: Long, y: Int = -550) =
        ShortsScroll(time, "pager", y, y, 0)

    @Test fun tenRealisticSwipesCountExactlyTenWithoutFingerprints() {
        val counter = ShortsCounter()
        assertNotNull(counter.update(viewer(), 0).incrementReason)
        var counts = 0
        for (index in 1..10) {
            val start = index * 800L
            counter.update(viewer(scroll = swipe(start)), start)
            counter.update(viewer(scroll = swipe(start + 100)), start + 100)
            counter.update(viewer(blocked = true), start + 180)
            counter.update(viewer(unknown = true), start + 220)
            if (counter.update(viewer(), start + 450).incrementReason != null) counts++
        }
        assertEquals(10, counts)
    }

    @Test fun scrollBurstAndIdentityChangeMergeToOneCount() {
        val counter = ShortsCounter()
        counter.update(viewer("a"), 0)
        counter.update(viewer("a", swipe(800)), 800)
        assertNotNull(counter.update(viewer("b"), 900).incrementReason)
        assertNull(counter.update(viewer("b"), 1_250).incrementReason)
    }

    @Test fun duplicateCallbacksAndHorizontalScrollDoNotCount() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        counter.update(viewer(scroll = swipe(800)), 800)
        assertNull(counter.update(viewer(scroll = swipe(800)), 810).incrementReason)
        assertNotNull(counter.update(viewer(), 1_200).incrementReason)
        counter.update(viewer(scroll = ShortsScroll(1_600, "pager", 0, 0, 80,
            relativeOffset = true)), 1_600)
        assertNull(counter.update(viewer(), 2_000).incrementReason)
    }

    @Test fun unknownFramesAndLoadingDoNotCancelPendingSwipe() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        counter.update(viewer(scroll = swipe(800)), 800)
        counter.update(viewer(unknown = true), 900)
        counter.update(viewer(blocked = true), 1_000)
        assertNotNull(counter.update(viewer(), 1_200).incrementReason)
    }

    @Test fun engagementPanelCancelsPendingButViewerEntryLatchSurvives() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        counter.update(viewer(scroll = swipe(800)), 800)
        counter.update(ShortsObservation(ShortsViewer.CONFIRMED, engagementPanelOpen = true), 900)
        assertNull(counter.update(viewer(), 1_300).incrementReason)
        counter.update(viewer(scroll = swipe(1_600)), 1_600)
        assertNotNull(counter.update(viewer(), 2_000).incrementReason)
    }

    @Test fun fastSevenHundredMillisecondSwipesCountEveryShort() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        var counts = 0
        for (index in 1..10) {
            val start = index * 700L
            counter.update(viewer(scroll = swipe(start)), start)
            if (counter.update(viewer(), start + 350).incrementReason != null) counts++
        }
        assertEquals(10, counts)
    }

    @Test fun sustainedOutsideResetsEntryButBriefLossDoesNot() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        counter.update(ShortsObservation(ShortsViewer.UNKNOWN), 500)
        assertNull(counter.update(viewer(), 600).incrementReason)
        counter.update(ShortsObservation(ShortsViewer.OUTSIDE), 1_000)
        counter.update(ShortsObservation(ShortsViewer.OUTSIDE), 3_100)
        assertNotNull(counter.update(viewer(), 3_200).incrementReason)
    }

    @Test fun singleScrollEventsCommitPreviousSwipeWithoutLosingCurrentSwipe() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        var counts = 0
        for (index in 1..10) {
            val time = index * 700L
            if (counter.update(viewer(scroll = swipe(time)), time).incrementReason != null) counts++
        }
        if (counter.update(viewer(), 7_350).incrementReason != null) counts++
        assertEquals(10, counts)
    }

    @Test fun zeroMovementLayoutScrollBurstsNeverCount() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        counter.update(viewer(scroll = ShortsScroll(100, "reel_recycler", 0, 0, 0)), 100)
        assertNull(counter.update(viewer(), 500).incrementReason)
        counter.update(viewer(scroll = ShortsScroll(1_000, "reel_player_underlay", 0, 0, 0)), 1_000)
        assertNull(counter.update(viewer(), 1_400).incrementReason)
    }

    @Test fun realThreeEventPatternCountsAtSevenHundredMillisecondIntervals() {
        assertEquals(10, countPattern(700L))
    }

    @Test fun realThreeEventPatternCountsAtTwentyFiveHundredMillisecondIntervals() {
        assertEquals(10, countPattern(2_500L))
    }

    private fun countPattern(interval: Long): Int {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        var counts = 0
        for (index in 1..10) {
            val start = index * interval
            val events = listOf(
                ShortsScroll(start, "reel_player_underlay", 926, 926, 0, relativeOffset = true),
                ShortsScroll(start + 50, "reel_player_underlay", 0, -926, 0, relativeOffset = true),
                ShortsScroll(start + 100, "reel_recycler", 0, 78, 0),
            )
            events.forEach { event ->
                if (counter.update(viewer(scroll = event), event.eventTime).incrementReason != null) counts++
            }
            if (counter.update(viewer(), start + 450).incrementReason != null) counts++
        }
        return counts
    }

    @Test fun batchedSixSwipeTraceUsesEventTimeAndPageSettledBoundaries() {
        val trace = listOf(
            ShortsScroll(1_945_248, "reel_player_underlay", 894, 894, 0, relativeOffset = true),
            ShortsScroll(1_945_375, "reel_player_underlay", -128, -1022, 0, relativeOffset = true),
            ShortsScroll(1_945_547, "reel_player_underlay", 0, 128, 0, relativeOffset = true),
            ShortsScroll(1_945_600, "reel_recycler", 0, 78, 0),
            ShortsScroll(1_946_195, "reel_player_underlay", 833, 833, 0, relativeOffset = true),
            ShortsScroll(1_946_331, "reel_player_underlay", -127, -960, 0, relativeOffset = true),
            ShortsScroll(1_946_439, "reel_player_underlay", 0, 127, 0, relativeOffset = true),
            ShortsScroll(1_947_293, "reel_player_underlay", 665, 665, 0, relativeOffset = true),
            ShortsScroll(1_947_637, "reel_player_underlay", 135, -530, 0, relativeOffset = true),
            ShortsScroll(1_947_753, "reel_player_underlay", 0, 135, 0, relativeOffset = true),
            ShortsScroll(1_948_332, "reel_player_underlay", 535, 535, 0, relativeOffset = true),
            ShortsScroll(1_948_472, "reel_player_underlay", 65, -470, 0, relativeOffset = true),
            ShortsScroll(1_948_609, "reel_player_underlay", 2, -63, 0, relativeOffset = true),
            ShortsScroll(1_948_711, "reel_player_underlay", 0, -2, 0, relativeOffset = true),
            ShortsScroll(1_949_297, "reel_player_underlay", 607, 607, 0, relativeOffset = true),
            ShortsScroll(1_949_463, "reel_player_underlay", 67, -540, 0, relativeOffset = true),
            ShortsScroll(1_949_616, "reel_player_underlay", 1, -66, 0, relativeOffset = true),
            ShortsScroll(1_949_728, "reel_player_underlay", 0, null, 0, relativeOffset = true),
            ShortsScroll(1_950_219, "reel_player_underlay", 670, 670, 0, relativeOffset = true),
            ShortsScroll(1_950_385, "reel_player_underlay", -158, -828, 0, relativeOffset = true),
            ShortsScroll(1_950_596, "reel_player_underlay", 0, 158, 0, relativeOffset = true),
        )
        assertEquals("batched", 6, countTrace(trace, batched = true))
        assertEquals("prompt", 6, countTrace(trace, batched = false))
    }

    private fun countTrace(trace: List<ShortsScroll>, batched: Boolean): Int {
        val counter = ShortsCounter()
        counter.update(viewer(), 1_945_000)
        var counts = 0
        trace.forEachIndexed { index, event ->
            val processingTime = if (batched) 10_000L + index * 3L else event.eventTime
            if (counter.update(viewer(scroll = event), processingTime).incrementReason != null) counts++
        }
        if (counter.update(viewer(), 20_000).incrementReason != null) counts++
        return counts
    }

    @Test fun delayedReturnAfterQuietFallbackCountsEachExcursionOnce() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        var counts = 0
        for (index in 1..10) {
            val start = index * 1_000L
            val begin = ShortsScroll(start, "reel_player_underlay", 887, 887, 0,
                relativeOffset = true)
            val end = ShortsScroll(start + 184, "reel_player_underlay", 0, -887, 0,
                relativeOffset = true)
            counter.update(viewer(scroll = begin), start)
            // A late CONTENT_CHANGED callback must not use processing time to settle this.
            assertNull(counter.update(viewer(), start + 400).incrementReason)
            if (counter.update(viewer(scroll = end), start + 500).incrementReason != null) counts++
        }
        assertEquals(10, counts)
    }

    @Test fun excursionWithoutReturnStillCountsOnceViaDelayedFallback() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        counter.update(viewer(scroll = ShortsScroll(1_000, "reel_player_underlay", 887, 887, 0,
            relativeOffset = true)), 1_000)
        assertNull(counter.update(viewer(), 1_400).incrementReason)
        assertNotNull(counter.update(viewer(), 2_000).incrementReason)
        assertNull(counter.update(viewer(scroll = ShortsScroll(2_184, "reel_player_underlay", 0,
            -887, 0, relativeOffset = true)), 2_184).incrementReason)
    }

    @Test fun overlayPauseAfterQuietFallbackResumesCurrentViewerWithoutDuplicateAndCountsNextSwipe() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)

        counter.update(viewer(scroll = ShortsScroll(1_000, "reel_player_underlay", 887, 887, 0,
            relativeOffset = true)), 1_000)
        assertNotNull(counter.update(viewer(), 2_000).incrementReason)

        // Continue with intention pauses only transient gesture state; the entered viewer remains latched.
        counter.suspend()
        assertNull(counter.update(viewer(), 2_100).incrementReason)

        val nextSwipeStart = ShortsScroll(2_184, "reel_player_underlay", 887, 887, 0,
            relativeOffset = true)
        counter.update(viewer(scroll = nextSwipeStart), 2_184)
        assertNull(counter.update(viewer(), 2_584).incrementReason)
        assertNotNull(counter.update(viewer(), 3_184).incrementReason)
        assertNull(counter.update(viewer(), 3_300).incrementReason)
    }
}
