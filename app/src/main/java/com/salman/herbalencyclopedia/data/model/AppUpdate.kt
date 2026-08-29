package com.salman.herbalencyclopedia.data.model

/**
 * Admin-editable update settings, stored in Firestore at "app_config/update"
 * (same project as the app's other data, editable from the app's admin panel).
 *
 * By default the app checks the GitHub repo below for its latest Release
 * and offers to download whichever asset ends with ".apk". The admin can
 * override any part of this from [AdminUpdateScreen] without shipping a new
 * build: point it at a different repo, force a specific download link,
 * override the shown version/notes, or require an update below a given
 * versionCode.
 */
data class AppUpdateConfig(
    val enabled: Boolean = true,
    val githubRepo: String = "slman14422003-create/Harbs",
    val overrideDownloadUrl: String? = null,
    val overrideVersionName: String? = null,
    val releaseNotesOverride: String? = null,
    val minVersionCode: Int = 0
)

/** Result of a successful update check: there IS a newer version available. */
data class AppUpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val releasePageUrl: String,
    val mandatory: Boolean
)
