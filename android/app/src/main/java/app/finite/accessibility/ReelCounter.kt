package app.finite.accessibility

/**
 * State machine that counts distinct Instagram Reels seen in a session.
 *
 * Identity dedupe uses a fingerprint produced by [ReelDetector] from stable per-Reel metadata
 * (author, caption). A new fingerprint must be observed [CONFIRMATIONS] consecutive times and be
 * separated from the last count by at least [DEBOUNCE_MS] before a Reel is counted. This prevents
 * engagement-label flicker and brief non-viewer frames from producing spurious counts.
 *
 * Counts are signalled via [update]'s returned [ReelDecision]. The service is responsible for
 * driving updates and for the [reset]/[suspend] lifecycle.
 */
internal class ReelCounter {
    private var inViewer = false
    private var entryCounted = false
    private var outsideSince: Long? = null
    private var lastFingerprint: String? = null
    private var pendingFingerprint: String? = null
    private var pendingHits = 0
    private var lastCountAt = 0L

    fun reset() {
        inViewer = false
        entryCounted = false
        outsideSince = null
        lastFingerprint = null
        pendingFingerprint = null
        pendingHits = 0
        lastCountAt = 0L
    }

    /**
     * Called when switching away from Instagram so that a brief app switch does not reset the
     * viewer entry latch. The outside timer starts so that a long absence still resets state.
     */
    fun platformSuspend(now: Long) {
        if (inViewer && outsideSince == null) outsideSince = now
        pendingFingerprint = null
        pendingHits = 0
    }

    /**
     * Process one [Detection] from [ReelDetector] and return a [ReelDecision] whose
     * [ReelDecision.incrementReason] is non-null when a new Reel should be counted.
     */
    fun update(detection: Detection, now: Long): ReelDecision {
        if (!detection.isReelsViewer) {
            if (outsideSince == null) outsideSince = now
            if (inViewer && now - outsideSince!! >= EXIT_MS) {
                inViewer = false
                entryCounted = false
            }
            pendingFingerprint = null
            pendingHits = 0
            return ReelDecision(detail = "not in Reels viewer")
        }

        // Viewer confirmed.
        val outsideDuration = outsideSince?.let { now - it } ?: 0L
        if (inViewer && outsideDuration >= EXIT_MS) {
            inViewer = false
            entryCounted = false
        }
        outsideSince = null

        if (!inViewer) {
            inViewer = true
            lastFingerprint = detection.fingerprint
            pendingFingerprint = null
            pendingHits = 0
            return if (detection.strongEvidence) {
                entryCounted = true
                lastCountAt = now
                ReelDecision("Reels viewer opened", "first Reel counted on strong evidence")
            } else {
                entryCounted = false
                ReelDecision(detail = "Reels viewer candidate — confirming identity")
            }
        }

        val fingerprint = detection.fingerprint
            ?: return ReelDecision(detail = "in viewer; no identity yet")

        // Latch identity if the entry was counted but fingerprint was absent at open time.
        if (entryCounted && lastFingerprint == null) {
            lastFingerprint = fingerprint
            pendingFingerprint = null
            pendingHits = 0
            return ReelDecision(detail = "identity latched post-entry")
        }

        if (fingerprint == lastFingerprint) {
            pendingFingerprint = null
            pendingHits = 0
            return ReelDecision(detail = "same Reel identity")
        }

        if (pendingFingerprint == fingerprint) {
            pendingHits += 1
        } else {
            pendingFingerprint = fingerprint
            pendingHits = 1
        }

        return if (pendingHits >= CONFIRMATIONS && now - lastCountAt >= DEBOUNCE_MS) {
            lastFingerprint = fingerprint
            pendingFingerprint = null
            pendingHits = 0
            entryCounted = true
            lastCountAt = now
            ReelDecision("confirmed Reel identity change", "fp=$fingerprint")
        } else {
            ReelDecision(detail = "pending identity confirmation (hits=$pendingHits)")
        }
    }

    companion object {
        internal const val EXIT_MS = 2_000L
        internal const val DEBOUNCE_MS = 1_200L
        internal const val CONFIRMATIONS = 2
    }
}

internal data class ReelDecision(val incrementReason: String? = null, val detail: String)
