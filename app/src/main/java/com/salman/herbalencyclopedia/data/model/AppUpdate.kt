package com.salman.herbalencyclopedia.data.model

/**
 * Admin-editable update settings, stored in Firestore at "app_config/update"
 * (same project as the app's other data, editable from the app's admin panel).
 *
 * The app checks the GitHub release metadata for version information. The
 * app is distributed as a direct APK download (not published on Google
 * Play), so an update hands the user straight to the .apk asset's download
 * link on GitHub, opened in the browser.
 */
data class AppUpdateConfig(
    val enabled: Boolean = true,
    val githubRepo: String = "slman14422003-create/Harbs",
    val overrideVersionName: String? = null,
    val releaseNotesOverride: String? = null,
    val minVersionCode: Int = 0
)

/** Result of a successful update check: there IS a newer version available. */
data class AppUpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val releasePageUrl: String,
    /** Direct download link to the .apk asset on the GitHub release, if one was attached. */
    val apkUrl: String? = null,
    val mandatory: Boolean
)
