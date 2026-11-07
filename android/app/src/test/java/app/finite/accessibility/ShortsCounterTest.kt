package app.finite.accessibility

import org.junit.Assert.*
import org.junit.Test

class ShortsCounterTest {
    private fun viewer(scroll: ShortsScroll? = null, fingerprint: String? = null) =
        ShortsObservation(ShortsViewer.CONFIRMED, scroll = scroll, fingerprint = fingerprint)
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
        for (time in listOf(1_200L, 1_300L, 1_500L, 1_800L)) {
            assertNull(counter.update(viewer(swipe(time)), time).incrementReason)
        }
        assertNull(counter.update(viewer(), 1_900).incrementReason)
        assertNull(counter.update(viewer(), 2_200).incrementReason)
        assertNotNull(counter.update(viewer(), 2_250).incrementReason)
        assertNull(counter.update(viewer(), 2_500).incrementReason)
        counter.update(viewer(swipe(1_800)), 3_000)
        assertNull(counter.nextCheckDelay(3_000))
    }

    @Test fun consecutiveForwardAndBackwardSwipesCountIncludingIdenticalGeometry() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        for ((time, y) in listOf(2_000L to -550, 4_000L to -550, 6_000L to 550)) {
            counter.update(viewer(swipe(time, y)), time)
            assertNotNull(counter.update(viewer(), time + 450).incrementReason)
        }
    }

    @Test fun pageIndicesDeduplicateAndPermitBackwardRevisits() {
        val counter = ShortsCounter()
        counter.update(viewer(swipe(0, 0, page = 0)), 0)
        counter.update(viewer(swipe(1_500, 0, page = 1)), 1_500)
        assertNotNull(counter.update(viewer(), 1_950).incrementReason)
        counter.update(viewer(swipe(3_000, 0, page = 1)), 3_000)
        assertNull(counter.update(viewer(), 3_450).incrementReason)
        counter.update(viewer(swipe(4_500, 0, page = 0)), 4_500)
        assertNotNull(counter.update(viewer(), 4_950).incrementReason)
    }

    @Test fun unchangedCumulativePositionsDoNotCountTwice() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        counter.update(viewer(swipe(1_500, relative = false)), 1_500)
        assertNotNull(counter.update(viewer(), 1_950).incrementReason)
        counter.update(viewer(swipe(3_000, relative = false)), 3_000)
        assertNull(counter.update(viewer(), 3_450).incrementReason)
    }

    @Test fun cancelledTinyAndHorizontalSwipesDoNotCount() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        counter.update(viewer(swipe(1_500)), 1_500)
        counter.update(viewer(ShortsScroll(1_600, "pager", 0, 550, relativeOffset = true)), 1_600)
        assertNull(counter.update(viewer(), 2_100).incrementReason)
        counter.update(viewer(swipe(3_000, 2)), 3_000)
        assertNull(counter.update(viewer(), 3_500).incrementReason)
        counter.update(viewer(ShortsScroll(4_500, "pager", 20, 20, 600)), 4_500)
        assertNull(counter.update(viewer(), 5_000).incrementReason)
    }

    @Test fun debounceRejectsImmediateLayoutScroll() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        // Scroll within count gap: queued as pending but gap prevents counting.
        counter.update(viewer(swipe(500)), 500)
        assertNotNull(counter.nextCheckDelay(500))   // pending exists; waiting for gap + settle
        // Additional scroll updates within the gap keep updating the pending gesture.
        counter.update(viewer(swipe(1_000)), 1_000)
        assertNotNull(counter.nextCheckDelay(1_000))
        // Scroll at gap boundary: gap now satisfied (1200 - 0 = 1200 >= MIN_COUNT_GAP_MS).
        counter.update(viewer(swipe(1_200)), 1_200)
        // After settle (1200 + 450 = 1650 ms) the pending commits exactly once.
        assertNotNull(counter.update(viewer(), 1_650).incrementReason)
    }

    @Test fun unavailablePositionUsesQualifiedScrollFallback() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        counter.update(viewer(ShortsScroll(1_500, "pager")), 1_500)
        assertNotNull(counter.update(viewer(), 1_950).incrementReason)
    }

    @Test fun temporaryViewerLossDoesNotRecountFirstShort() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        counter.update(ShortsObservation(ShortsViewer.UNKNOWN), 1_000)
        assertNull(counter.update(viewer(), 2_000).incrementReason)
        counter.update(ShortsObservation(ShortsViewer.OUTSIDE), 3_000)
        assertNull(counter.update(viewer(), 4_500).incrementReason)
        counter.update(ShortsObservation(ShortsViewer.OUTSIDE), 5_000)
        counter.update(ShortsObservation(ShortsViewer.OUTSIDE), 7_100)
        assertNotNull(counter.update(viewer(), 7_200).incrementReason)
    }

    @Test fun panelsAndAppSwitchCancelPendingSwipesAndPreserveEntryLatch() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        counter.update(viewer(swipe(1_500)), 1_500)
        counter.update(ShortsObservation(ShortsViewer.CONFIRMED, blocked = true), 1_600)
        assertNull(counter.update(viewer(), 2_100).incrementReason)
        counter.update(viewer(swipe(3_000)), 3_000)
        counter.suspend()
        assertNull(counter.nextCheckDelay(3_100))
        assertNull(counter.update(viewer(), 3_500).incrementReason)
    }

    @Test fun sessionResetClearsEntryAndPendingEvents() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        counter.update(viewer(swipe(1_500)), 1_500)
        counter.reset()
        assertNull(counter.nextCheckDelay(1_501))
        assertNotNull(counter.update(viewer(), 1_501).incrementReason)
    }

    @Test fun ordinaryVideosAndGenericOutsideScrollsCannotCount() {
        val counter = ShortsCounter()
        for (time in 1_000L..5_000L step 1_000) {
            assertNull(counter.update(ShortsObservation(ShortsViewer.OUTSIDE, scroll = swipe(time)), time)
                .incrementReason)
        }
    }

    @Test fun staleScrollFingerprintsCountOnlyOnceWhenNewIdentityConfirmed() {
        val counter = ShortsCounter()
        counter.update(viewer(fingerprint = "short-A"), 0)
        // User initiates swipe while stale fingerprint "short-A" is still present
        counter.update(viewer(scroll = swipe(1_500), fingerprint = "short-A"), 1_500)
        // Window content updates to new Short "short-B"
        counter.update(viewer(fingerprint = "short-B"), 1_700)
        // Wait for quiet period (1500 + 450 = 1950)
        assertNotNull(counter.update(viewer(fingerprint = "short-B"), 1_950).incrementReason)
        // Subsequent events with the same fingerprint "short-B" do not count again
        assertNull(counter.update(viewer(fingerprint = "short-B"), 2_500).incrementReason)
        // Partial swipe that bounces back to same fingerprint "short-B" does not count
        counter.update(viewer(scroll = swipe(3_500), fingerprint = "short-B"), 3_500)
        assertNull(counter.update(viewer(fingerprint = "short-B"), 3_950).incrementReason)
    }

    @Test fun rapidRepeatedEventsWithinQuietPeriodDoNotTriggerMultipleCounts() {
        val counter = ShortsCounter()
        counter.update(viewer(), 0)
        // Burst of rapid scroll updates during a single swipe gesture
        for (time in listOf(1_500L, 1_550L, 1_600L, 1_650L, 1_700L)) {
            assertNull(counter.update(viewer(swipe(time)), time).incrementReason)
        }
        // Still settling within the 450ms quiet period (1700 + 450 = 2150)
        assertNull(counter.update(viewer(), 1_900).incrementReason)
        assertNull(counter.update(viewer(), 2_100).incrementReason)
        // Settled: counts exactly once
        assertNotNull(counter.update(viewer(), 2_150).incrementReason)
        // After settling, repeated calls without new scroll do not count
        assertNull(counter.update(viewer(), 2_200).incrementReason)
        assertNull(counter.update(viewer(), 3_000).incrementReason)
    }

    @Test fun identityFallbackUsesFingerprintWhenPageNumberIsAbsent() {
        val counter = ShortsCounter()
        // First short with fingerprint fp-alpha and no page index
        counter.update(viewer(fingerprint = "fp-alpha"), 0)
        // Swipe to new Short with fingerprint fp-beta
        counter.update(viewer(scroll = swipe(1_500), fingerprint = "fp-beta"), 1_500)
        assertNotNull(counter.update(viewer(fingerprint = "fp-beta"), 1_950).incrementReason)
        // Swipe with identical fingerprint fp-beta is deduplicated
        counter.update(viewer(scroll = swipe(3_500), fingerprint = "fp-beta"), 3_500)
        assertNull(counter.update(viewer(fingerprint = "fp-beta"), 3_950).incrementReason)
        // Swipe back to fp-alpha counts
        counter.update(viewer(scroll = swipe(5_500), fingerprint = "fp-alpha"), 5_500)
        assertNotNull(counter.update(viewer(fingerprint = "fp-alpha"), 5_950).incrementReason)
    }

    @Test fun scrollWithinCountGapIsQueuedNotDiscardedIdentityCaptured() {
        // Regression: a swipe starting within MIN_COUNT_GAP_MS of the last count must NOT
        // be silently discarded; it should build a pending gesture and commit once the gap
        // and settle periods have both elapsed.
        val counter = ShortsCounter()
        counter.update(viewer(fingerprint = "fp-first"), 0)   // entry; lastCountAt = 0
        // Swipe arrives at t=600 — within the 1200 ms count gap.
        counter.update(viewer(scroll = swipe(600), fingerprint = "fp-second"), 600)
        // The pending gesture must exist (follow-up delay > 0).
        assertNotNull("pending gesture must exist for scroll within gap", counter.nextCheckDelay(600))
        // Gap passes at t=1200. No settle action happens yet since lastAt=600, settle at 1050.
        // Both gap (>=1200 from t=0) and settle (>=450 from t=600) are satisfied at t=1200.
        assertNotNull("swipe within gap must count after gap + settle",
            counter.update(viewer(fingerprint = "fp-second"), 1_200).incrementReason)
    }
}
