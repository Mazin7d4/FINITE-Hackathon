package app.finite.bridge

import app.finite.data.SessionStore

/** Returns the first installed selected app in the deterministic launch order. */
internal fun selectLaunchPackage(
    selectedApps: Set<String>,
    isInstalled: (String) -> Boolean,
): String? = listOf(
    SessionStore.INSTAGRAM to SessionStore.INSTAGRAM_PACKAGE,
    SessionStore.YOUTUBE to SessionStore.YOUTUBE_PACKAGE,
).firstOrNull { (app, packageName) -> app in selectedApps && isInstalled(packageName) }?.second
