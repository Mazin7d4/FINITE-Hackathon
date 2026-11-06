package app.finite.accessibility

import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

internal data class Detection(
    val isReelsViewer: Boolean,
    val fingerprint: String?,
    val strongEvidence: Boolean,
    val actionScore: Int,
    val sourceLooksScrollable: Boolean,
    val summary: String,
)

/**
 * Conservative, metadata-only classifier for Instagram's Reels viewer.
 *
 * Instagram does not publish a Reel transition API and its internal view IDs vary by release.
 * This detector therefore requires either a Reels/"clips" viewer resource ID, or a combination
 * of a visible Reels label and several viewer actions. It never treats a generic event as a Reel.
 */
internal class ReelDetector {
    fun inspect(event: AccessibilityEvent, root: AccessibilityNodeInfo?): Detection {
        if (root == null) {
            return Detection(false, null, false, 0, false, "no window root")
        }

        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        var strongIdHits = 0
        var reelsLabels = 0
        val actions = linkedSetOf<String>()
        val authors = linkedSetOf<String>()
        val captions = linkedSetOf<String>()
        val fallbackIdentity = linkedSetOf<String>()

        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited += 1
            val id = node.viewIdResourceName.orEmpty().lowercase()
            val text = node.text?.toString()?.trim().orEmpty()
            val description = node.contentDescription?.toString()?.trim().orEmpty()

            if (id.isStrongViewerId()) strongIdHits += 1
            listOf(text, description).filter { it.isNotBlank() }.forEach { value ->
                val normalized = value.lowercase()
                if (normalized == "reels" || normalized == "reel") reelsLabels += 1
                actionName(normalized)?.let(actions::add)

                when {
                    id.hasAny("username", "author", "owner_name", "profile_name") &&
                        value.isIdentityText() -> authors.add(value.normalizedIdentity())
                    id.hasAny("caption", "description", "media_text") &&
                        value.isIdentityText() -> captions.add(value.normalizedIdentity())
                    value.isIdentityText() -> fallbackIdentity.add(value.normalizedIdentity())
                }
            }

            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::addLast)
            }
        }

        val strong = strongIdHits > 0
        val combinedEvidence = reelsLabels > 0 && actions.size >= REQUIRED_ACTIONS
        val isViewer = strong || combinedEvidence
        val identityParts = buildList {
            addAll(authors.take(2))
            addAll(captions.take(2))
            if (isEmpty() && isViewer) addAll(fallbackIdentity.take(3))
        }
        val fingerprint = identityParts
            .takeIf { it.isNotEmpty() }
            ?.joinToString("|")
            ?.hashCode()
            ?.toUInt()
            ?.toString(16)

        return Detection(
            isReelsViewer = isViewer,
            fingerprint = fingerprint,
            strongEvidence = strong,
            actionScore = actions.size,
            sourceLooksScrollable = sourceLooksLikeReelPager(event.source),
            summary = "viewer=$isViewer strongIds=$strongIdHits actions=${actions.size} " +
                "labels=$reelsLabels fp=${fingerprint ?: "none"} nodes=$visited",
        )
    }

    private fun sourceLooksLikeReelPager(source: AccessibilityNodeInfo?): Boolean {
        var current = source
        var depth = 0
        while (current != null && depth < 5) {
            val id = current.viewIdResourceName.orEmpty().lowercase()
            val className = current.className?.toString().orEmpty().lowercase()
            if (id.isStrongViewerId() &&
                (current.isScrollable || className.hasAny("viewpager", "recyclerview"))
            ) return true
            val parent = current.parent
            if (current !== source && Build.VERSION.SDK_INT < 33) current.recycle()
            current = parent
            depth += 1
        }
        return false
    }

    private fun String.isStrongViewerId(): Boolean {
        return (contains("clips") && hasAny("viewer", "video", "swipe", "pager", "media")) ||
            (contains("reel") && hasAny("viewer", "video", "swipe", "pager"))
    }

    private fun actionName(value: String): String? = when {
        value == "like" || value.startsWith("like ") || value.startsWith("likes") -> "like"
        value == "comment" || value.startsWith("comment") -> "comment"
        value == "share" || value.startsWith("share") || value.startsWith("send") -> "share"
        value == "follow" || value.startsWith("follow ") -> "follow"
        value.contains("audio") || value.contains("original sound") -> "audio"
        else -> null
    }

    private fun String.hasAny(vararg values: String): Boolean = values.any(::contains)

    private fun String.isIdentityText(): Boolean {
        val normalized = lowercase().trim()
        if (normalized.length !in 2..160) return false
        if (normalized.all { it.isDigit() || it.isWhitespace() || it in ",." }) return false
        if (actionName(normalized) != null) return false
        return normalized !in IGNORED_TEXT && !normalized.startsWith("reels")
    }

    private fun String.normalizedIdentity(): String =
        lowercase().replace(Regex("\\s+"), " ").take(160)

    companion object {
        private const val MAX_NODES = 350
        private const val REQUIRED_ACTIONS = 3
        private val IGNORED_TEXT = setOf(
            "home", "search", "explore", "profile", "notifications", "more", "back",
            "play", "pause", "mute", "unmute", "instagram", "create", "shop",
        )
    }
}

