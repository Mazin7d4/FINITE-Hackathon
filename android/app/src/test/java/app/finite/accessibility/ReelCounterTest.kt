package app.finite.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ReelCounterTest {
    private fun viewer(fp: String? = null, scroll: ReelScroll? = null, strong: Boolean = true,
                       strongFingerprint: Boolean = false) =
        Detection(true, fp, strong, 3, true, "", scroll, strongFingerprint)
    private fun outside() = Detection(false, null, false, 0, false, "")
    private fun swipe(time: Long, y: Int = -550) = ReelScroll(time, y, y, 0)

    @Test fun entryCountsOnceAndMissingFingerprintsDoNotBlockSwipes() {
        val counter = ReelCounter()
        assertNotNull(counter.update(viewer(), 0).incrementReason)
        var counts = 1
        for (index in 1..10) {
            val start = index * 800L
            assertNull(counter.update(viewer(scroll = swipe(start)), start).incrementReason)
            assertNull(counter.update(viewer(scroll = swipe(start + 100)), start + 100).incrementReason)
            if (counter.update(viewer(), start + 450).incrementReason != null) counts++
        }
        assertEquals(11, counts)
    }

    @Test fun scrollBurstAndIdentityChangeCountOnce() {
        val counter = ReelCounter()
        counter.update(viewer("a", strongFingerprint = true), 0)
        counter.update(viewer("a", swipe(800), strongFingerprint = true), 800)
        val identity = counter.update(viewer("b", strongFingerprint = true), 900)
        assertNotNull(identity.incrementReason)
        assertNull(counter.update(viewer("b"), 1_250).incrementReason)
    }

    @Test fun duplicateCallbacksAndHorizontalScrollAreRejected() {
        val counter = ReelCounter()
        counter.update(viewer(), 0)
        counter.update(viewer(scroll = swipe(800)), 800)
        assertNull(counter.update(viewer(scroll = swipe(800)), 810).incrementReason)
        assertNotNull(counter.update(viewer(), 1_200).incrementReason)
        counter.update(viewer(scroll = ReelScroll(1_600, 0, 0, 80)), 1_600)
        assertNull(counter.update(viewer(), 2_000).incrementReason)
    }

    @Test fun transientExitDoesNotCancelPendingButSustainedExitResetsEntry() {
        val counter = ReelCounter()
        counter.update(viewer(), 0)
        counter.update(viewer(scroll = swipe(800)), 800)
        counter.update(outside(), 900)
        assertNotNull(counter.update(viewer(), 1_200).incrementReason)
        counter.update(outside(), 2_000)
        counter.update(outside(), 4_100)
        assertNotNull(counter.update(viewer(), 4_200).incrementReason)
    }

    @Test fun fastSwipesAtSevenHundredMillisecondsCountEveryReel() {
        val counter = ReelCounter()
        counter.update(viewer(), 0)
        var counts = 0
        for (index in 1..10) {
            val start = index * 700L
            counter.update(viewer(scroll = swipe(start)), start)
            if (counter.update(viewer(), start + 350).incrementReason != null) counts++
        }
        assertEquals(10, counts)
    }

    @Test fun singleScrollEventsCommitPreviousSwipeWithoutLosingCurrentSwipe() {
        val counter = ReelCounter()
        counter.update(viewer(), 0)
        var counts = 0
        for (index in 1..10) {
            val time = index * 700L
            if (counter.update(viewer(scroll = swipe(time)), time).incrementReason != null) counts++
        }
        if (counter.flush(7_350).incrementReason != null) counts++
        assertEquals(10, counts)
    }

    @Test fun fallbackFingerprintFlickerWithoutScrollDoesNotCount() {
        val counter = ReelCounter()
        counter.update(viewer("fallback-a"), 0)
        for ((time, fingerprint) in listOf(500L to "fallback-b", 1_000L to "fallback-c",
            1_500L to "fallback-d")) {
            assertNull(counter.update(viewer(fingerprint), time).incrementReason)
        }
    }

    @Test fun zeroMovementLayoutScrollBurstsNeverCount() {
        val counter = ReelCounter()
        counter.update(viewer(), 0)
        counter.update(viewer(scroll = ReelScroll(100, 0, 0, 0)), 100)
        assertNull(counter.update(viewer(), 500).incrementReason)
        counter.update(viewer(scroll = ReelScroll(1_000, 0, 0, 0)), 1_000)
        assertNull(counter.update(viewer(), 1_400).incrementReason)
    }

    @Test fun realThreeEventPatternCountsAtSevenHundredMillisecondIntervals() {
        assertEquals(10, countPattern(700L))
    }

    @Test fun realThreeEventPatternCountsAtTwentyFiveHundredMillisecondIntervals() {
        assertEquals(10, countPattern(2_500L))
    }

    private fun countPattern(interval: Long): Int {
        val counter = ReelCounter()
        counter.update(viewer(), 0)
        var counts = 0
        for (index in 1..10) {
            val start = index * interval
            val events = listOf(
                ReelScroll(start, 926, 926, 0),
                ReelScroll(start + 50, 0, -926, 0),
                ReelScroll(start + 100, 0, 78, 0),
            )
            events.forEach { event ->
                if (counter.update(viewer(scroll = event), event.eventTime).incrementReason != null) counts++
            }
            if (counter.update(viewer(), start + 450).incrementReason != null) counts++
        }
        return counts
    }
}
