package com.salman.herbalencyclopedia.data.model

/**
 * Admin-editable update settings, stored in Firestore at "app_config/update"
 * (same project as the app's other data, editable from the app's admin panel).
 *
 * The app checks the GitHub release metadata for version information. Actual
 * installation is always delegated to Google Play so the app never side-loads
 * an APK or requests REQUEST_INSTALL_PACKAGES.
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
    val mandatory: Boolean
)
