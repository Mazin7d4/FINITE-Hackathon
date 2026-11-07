package app.finite.accessibility

import org.junit.Assert.*
import org.junit.Test

class ShortsCounterTest {
    private fun viewer(scroll: ShortsScroll? = null) = ShortsObservation(ShortsViewer.CONFIRMED, scroll = scroll)
    private fun swipe(time: Long, y: Int = -550, page: Int? = null, relative: Boolean = true) =
        ShortsScroll(time, "pager", y, deltaY = y, pageIndex = page, relativeOffset = relative)

    @Test fun firstShortCountsOnceAndContentUpdatesDoNotCount() {
        val counter = ShortsCounter()
        assertNotNull(counter.update(viewer(), 0).incrementReason)
        for (time in 1_000L..10_000L step 500) assertNull(counter.update(viewer(), time).incrementReason)
    }

    @Test fun oneSwipeBurstIncludingDuplicateCallbacksCountsOnce() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        for (time in listOf(1_000L, 1_100L, 1_500L, 1_800L)) {
            assertNull(counter.update(viewer(swipe(time)), time).incrementReason)
        }
        assertNull(counter.update(viewer(), 1_900).incrementReason)
        assertNotNull(counter.update(viewer(), 2_000).incrementReason)
        assertNull(counter.update(viewer(), 2_500).incrementReason)
        counter.update(viewer(swipe(1_800)), 3_000)
        assertNull(counter.nextCheckDelay(3_000))
    }

    @Test fun consecutiveForwardAndBackwardSwipesCountIncludingIdenticalGeometry() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        for ((time, y) in listOf(1_000L to -550, 2_000L to -550, 3_000L to 550)) {
            counter.update(viewer(swipe(time, y)), time)
            assertNotNull(counter.update(viewer(), time + 200).incrementReason)
        }
    }

    @Test fun pageIndicesDeduplicateAndPermitBackwardRevisits() {
        val counter = ShortsCounter()
        counter.update(viewer(swipe(0, 0, page = 0)), 0)
        counter.update(viewer(swipe(1_000, 0, page = 1)), 1_000)
        assertNotNull(counter.update(viewer(), 1_200).incrementReason)
        counter.update(viewer(swipe(2_000, 0, page = 1)), 2_000)
        assertNull(counter.update(viewer(), 2_200).incrementReason)
        counter.update(viewer(swipe(3_000, 0, page = 0)), 3_000)
        assertNotNull(counter.update(viewer(), 3_200).incrementReason)
    }

    @Test fun unchangedCumulativePositionsDoNotCountTwice() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        counter.update(viewer(swipe(1_000, relative = false)), 1_000)
        assertNotNull(counter.update(viewer(), 1_200).incrementReason)
        counter.update(viewer(swipe(2_000, relative = false)), 2_000)
        assertNull(counter.update(viewer(), 2_200).incrementReason)
    }

    @Test fun cancelledTinyAndHorizontalSwipesDoNotCount() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        counter.update(viewer(swipe(1_000)), 1_000)
        counter.update(viewer(ShortsScroll(1_100, "pager", 0, 550, relativeOffset = true)), 1_100)
        assertNull(counter.update(viewer(), 1_300).incrementReason)
        counter.update(viewer(swipe(2_000, 2)), 2_000)
        assertNull(counter.update(viewer(), 2_200).incrementReason)
        counter.update(viewer(ShortsScroll(3_000, "pager", 20, 20, 600)), 3_000)
        assertNull(counter.update(viewer(), 3_200).incrementReason)
    }

    @Test fun debounceRejectsImmediateLayoutScroll() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        counter.update(viewer(swipe(500)), 500)
        assertNull(counter.nextCheckDelay(500))
        counter.update(viewer(swipe(700)), 700)
        assertNotNull(counter.update(viewer(), 900).incrementReason)
    }

    @Test fun unavailablePositionUsesQualifiedScrollFallback() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        counter.update(viewer(ShortsScroll(1_000, "pager")), 1_000)
        assertNotNull(counter.update(viewer(), 1_200).incrementReason)
    }

    @Test fun temporaryViewerLossDoesNotRecountFirstShort() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        counter.update(ShortsObservation(ShortsViewer.UNKNOWN), 1_000)
        assertNull(counter.update(viewer(), 2_000).incrementReason)
        counter.update(ShortsObservation(ShortsViewer.OUTSIDE), 3_000)
        assertNull(counter.update(viewer(), 3_200).incrementReason)
        counter.update(ShortsObservation(ShortsViewer.OUTSIDE), 4_000)
        counter.update(ShortsObservation(ShortsViewer.OUTSIDE), 4_600)
        assertNotNull(counter.update(viewer(), 5_000).incrementReason)
    }

    @Test fun panelsAndAppSwitchCancelPendingSwipesAndPreserveEntryLatch() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        counter.update(viewer(swipe(1_000)), 1_000)
        counter.update(ShortsObservation(ShortsViewer.CONFIRMED, blocked = true), 1_100)
        assertNull(counter.update(viewer(), 1_500).incrementReason)
        counter.update(viewer(swipe(2_000)), 2_000)
        counter.suspend()
        assertNull(counter.nextCheckDelay(2_100))
        assertNull(counter.update(viewer(), 2_500).incrementReason)
    }

    @Test fun sessionResetClearsEntryAndPendingEvents() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        counter.update(viewer(swipe(1_000)), 1_000)
        counter.reset()
        assertNull(counter.nextCheckDelay(1_001))
        assertNotNull(counter.update(viewer(), 1_001).incrementReason)
    }

    @Test fun ordinaryVideosAndGenericOutsideScrollsCannotCount() {
        val counter = ShortsCounter()
        for (time in 1_000L..5_000L step 1_000) {
            assertNull(counter.update(ShortsObservation(ShortsViewer.OUTSIDE, scroll = swipe(time)), time)
                .incrementReason)
        }
    }
}
