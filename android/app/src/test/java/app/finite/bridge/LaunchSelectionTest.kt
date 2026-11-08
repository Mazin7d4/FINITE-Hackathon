package app.finite.bridge

import app.finite.data.SessionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LaunchSelectionTest {
    @Test fun instagramWinsWhenBothSelectedAndInstalled() {
        val installed = setOf(SessionStore.INSTAGRAM_PACKAGE, SessionStore.YOUTUBE_PACKAGE)

        assertEquals(
            SessionStore.INSTAGRAM_PACKAGE,
            selectLaunchPackage(SessionStore.SUPPORTED_APPS) { it in installed },
        )
    }

    @Test fun selectedYoutubeIsFallbackWhenInstagramIsUnavailable() {
        assertEquals(
            SessionStore.YOUTUBE_PACKAGE,
            selectLaunchPackage(SessionStore.SUPPORTED_APPS) { it == SessionStore.YOUTUBE_PACKAGE },
        )
    }

    @Test fun unselectedInstalledAppsAreExcludedFromLaunch() {
        assertNull(
            selectLaunchPackage(setOf(SessionStore.INSTAGRAM)) { it == SessionStore.YOUTUBE_PACKAGE },
        )
    }
}
