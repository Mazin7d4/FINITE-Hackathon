package app.finite.accessibility

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [ReelCounter] — the Instagram Reel counting and identity-dedupe state machine.
 *
 * These tests cover service-level count state: viewer entry, reentry after absence, fingerprint
 * confirmation/debounce, and platform-switch suspension. They are the regression tests previously
 * absent (the existing ReelDetectorTest only tested classification, not counting state).
 */
class ReelCounterTest {

    // --- helpers -----------------------------------------------------------

    private fun strong(fp: String? = null) =
        Detection(isReelsViewer = true, fingerprint = fp, strongEvidence = true,
            actionScore = 0, sourceLooksScrollable = true, summary = "")

    private fun weak(fp: String? = null) =
        Detection(isReelsViewer = true, fingerprint = fp, strongEvidence = false,
            actionScore = 3, sourceLooksScrollable = true, summary = "")

    private fun outside() =
        Detection(isReelsViewer = false, fingerprint = null, strongEvidence = false,
            actionScore = 0, sourceLooksScrollable = false, summary = "")

    // --- viewer entry ------------------------------------------------------

    @Test fun strongEvidenceCountsOnFirstObservation() {
        val counter = ReelCounter()
        val decision = counter.update(strong("fp-A"), 0)
        assertNotNull("strong first observation must count", decision.incrementReason)
    }

    @Test fun weakEvidenceDoesNotCountOnFirstObservation() {
        val counter = ReelCounter()
        val decision = counter.update(weak("fp-A"), 0)
        assertNull("weak first observation must not count", decision.incrementReason)
    }

    @Test fun strongEntryFollowedByContentEventsDoNotDoubleCount() {
        val counter = ReelCounter()
        counter.update(strong("fp-A"), 0)
        for (t in 1_000L..5_000L step 500) {
            assertNull("repeated same-reel events must not count",
                counter.update(strong("fp-A"), t).incrementReason)
        }
    }

    // --- identity confirmation and debounce --------------------------------

    @Test fun newFingerprintCountsAfterTwoHitsAndDebounce() {
        val counter = ReelCounter()
        counter.update(strong("fp-A"), 0)
        // First hit of fp-B: not yet confirmed.
        assertNull(counter.update(strong("fp-B"), 1_200).incrementReason)
        // Second hit of fp-B after debounce: confirmed.
        assertNotNull(counter.update(strong("fp-B"), 2_400).incrementReason)
    }

    @Test fun singleHitBeforeDebounceDoesNotCount() {
        val counter = ReelCounter()
        counter.update(strong("fp-A"), 0)
        // Only one hit, within debounce window.
        assertNull(counter.update(strong("fp-B"), 500).incrementReason)
        // Still only one hit at debounce boundary.
        assertNull(counter.update(strong("fp-A"), 1_200).incrementReason)
    }

    @Test fun flickeringFingerprintResetsHitCountAndDoesNotCount() {
        val counter = ReelCounter()
        counter.update(strong("fp-A"), 0)
        // fp-B appears, then fp-A interrupts: pending hit count for fp-B resets.
        counter.update(strong("fp-B"), 1_500)  // pendingHits(fp-B) = 1
        counter.update(strong("fp-A"), 2_000)  // fp-A == lastFingerprint → pending cleared
        // fp-B starts fresh: only 1 hit now.
        assertNull("single hit after interruption must not count",
            counter.update(strong("fp-B"), 2_500).incrementReason) // pendingHits(fp-B) = 1
        // Second consecutive hit of fp-B after debounce: counts.
        assertNotNull("two consecutive hits after debounce must count",
            counter.update(strong("fp-B"), 3_200).incrementReason) // pendingHits(fp-B) = 2 ✓
    }

    @Test fun confirmedFingerprintDoesNotCountAgainWhenRevisited() {
        val counter = ReelCounter()
        counter.update(strong("fp-A"), 0)
        counter.update(strong("fp-B"), 1_200)
        counter.update(strong("fp-B"), 2_400) // counts fp-B
        // Return to fp-A: counts (it's a different reel).
        counter.update(strong("fp-A"), 3_600)
        assertNotNull(counter.update(strong("fp-A"), 4_800).incrementReason)
        // Stay on fp-A — same reel, must not count again.
        assertNull(counter.update(strong("fp-A"), 6_000).incrementReason)
    }

    // --- absent fingerprint ------------------------------------------------

    @Test fun identityLatchedAfterStrongEntryWithoutFingerprint() {
        val counter = ReelCounter()
        // Strong entry with no fingerprint yet (viewer opened before metadata loads).
        counter.update(strong(fp = null), 0)
        // Fingerprint arrives: latched without counting again.
        assertNull(counter.update(strong("fp-A"), 500).incrementReason)
        // Same fingerprint: still no count.
        assertNull(counter.update(strong("fp-A"), 1_000).incrementReason)
        // New fingerprint after latch: requires confirmation.
        counter.update(strong("fp-B"), 2_000)
        assertNotNull(counter.update(strong("fp-B"), 3_200).incrementReason)
    }

    // --- viewer exit and reentry -------------------------------------------

    @Test fun briefAbsenceDoesNotResetViewerLatch() {
        val counter = ReelCounter()
        counter.update(strong("fp-A"), 0)
        // Brief outside observation (< EXIT_MS = 2000 ms).
        counter.update(outside(), 500)
        counter.update(outside(), 1_000)
        // Returns before EXIT_MS: should not count viewer entry again.
        assertNull(counter.update(strong("fp-A"), 1_500).incrementReason)
    }

    @Test fun longAbsenceResetsViewerLatchAndCountsReentry() {
        val counter = ReelCounter()
        counter.update(strong("fp-A"), 0)
        // Stay outside for longer than EXIT_MS (2000 ms).
        counter.update(outside(), 500)
        counter.update(outside(), 2_600) // triggers latch reset
        // Reenter viewer: strong evidence → counts again.
        assertNotNull(counter.update(strong("fp-A"), 2_700).incrementReason)
    }

    @Test fun reentryAfterLongAbsenceCountsEvenWithSameFingerprint() {
        val counter = ReelCounter()
        counter.update(strong("fp-A"), 0)
        counter.update(outside(), 500)
        counter.update(outside(), 2_600)
        // Reenter with the SAME fingerprint — still a fresh entry.
        val reentry = counter.update(strong("fp-A"), 2_700)
        assertNotNull("reentry with same fp after long absence must count", reentry.incrementReason)
    }

    @Test fun platformSwitchStartsExitTimerWithoutImmediateReset() {
        val counter = ReelCounter()
        counter.update(strong("fp-A"), 0)
        // Platform switch (e.g. user opens YouTube briefly).
        counter.platformSuspend(500)
        // Return quickly — no reset yet (< EXIT_MS).
        assertNull(counter.update(strong("fp-A"), 1_000).incrementReason)
    }

    @Test fun platformSwitchFollowedByLongAbsenceResetsLatch() {
        val counter = ReelCounter()
        counter.update(strong("fp-A"), 0)
        counter.platformSuspend(500) // outsideSince = 500
        // Return after EXIT_MS from the suspend point.
        counter.update(outside(), 2_600) // triggers reset via update
        assertNotNull("strong reentry after platform switch + long absence must count",
            counter.update(strong("fp-A"), 2_700).incrementReason)
    }

    // --- session reset -----------------------------------------------------

    @Test fun resetClearsAllStateAndAllowsFreshEntry() {
        val counter = ReelCounter()
        counter.update(strong("fp-A"), 0)
        counter.update(strong("fp-B"), 1_200)
        counter.update(strong("fp-B"), 2_400) // commits fp-B
        counter.reset()
        // After reset: strong entry counts again from scratch.
        assertNotNull(counter.update(strong("fp-A"), 3_000).incrementReason)
        // And the same fingerprint seen again doesn't skip the confirmation phase.
        assertNull(counter.update(strong("fp-B"), 4_200).incrementReason)
        assertNotNull(counter.update(strong("fp-B"), 5_400).incrementReason)
    }

    // --- weak evidence path ------------------------------------------------

    @Test fun weakEntryDoesNotCountUntilNewFingerprintIsConfirmed() {
        val counter = ReelCounter()
        // Weak entry: candidate state; does NOT count the entry immediately.
        counter.update(weak("fp-A"), 0)
        // Same fingerprint — viewer is confirmed but no identity transition yet.
        assertNull(counter.update(weak("fp-A"), 500).incrementReason)
        // New fingerprint fp-B appears and is confirmed over two events.
        counter.update(weak("fp-B"), 1_500)
        assertNotNull("confirmed identity change after weak entry must count",
            counter.update(weak("fp-B"), 2_700).incrementReason)
        // Stay on fp-B: no recount.
        assertNull(counter.update(weak("fp-B"), 3_000).incrementReason)
    }
}
