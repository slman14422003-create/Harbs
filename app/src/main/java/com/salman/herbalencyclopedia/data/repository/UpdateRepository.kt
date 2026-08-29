package com.salman.herbalencyclopedia.data.repository

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.salman.herbalencyclopedia.data.model.AppUpdateConfig
import com.salman.herbalencyclopedia.data.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Handles in-app self-update:
 *  - reads admin-editable settings from Firestore ("app_config/update"),
 *  - checks the configured GitHub repo's latest Release automatically,
 *  - downloads the APK asset with the system DownloadManager (shows in the
 *    system notification shade, resumable, no extra permissions needed),
 *  - and launches the package installer for it.
 *
 * This mirrors the same "settings document editable by the admin panel"
 * pattern the web app already uses for its own config.
 */
class UpdateRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val DEFAULT_REPO = "slman14422003-create/Harbs"
        private const val CONFIG_COLLECTION = "app_config"
        private const val CONFIG_DOC = "update"
    }

    // ------------------------------------------------------------------
    // Admin-editable configuration
    // ------------------------------------------------------------------

    suspend fun fetchConfig(): AppUpdateConfig = withContext(Dispatchers.IO) {
        try {
            val doc = db.collection(CONFIG_COLLECTION).document(CONFIG_DOC).get(Source.DEFAULT).await()
            if (doc.exists()) {
                AppUpdateConfig(
                    enabled = doc.getBoolean("enabled") ?: true,
                    githubRepo = doc.getString("github_repo")?.trim()?.takeIf { it.isNotBlank() } ?: DEFAULT_REPO,
                    overrideDownloadUrl = doc.getString("override_download_url")?.trim()?.takeIf { it.isNotBlank() },
                    overrideVersionName = doc.getString("override_version_name")?.trim()?.takeIf { it.isNotBlank() },
                    releaseNotesOverride = doc.getString("release_notes_override")?.trim()?.takeIf { it.isNotBlank() },
                    minVersionCode = (doc.getLong("min_version_code") ?: 0L).toInt()
                )
            } else {
                AppUpdateConfig(githubRepo = DEFAULT_REPO)
            }
        } catch (e: Exception) {
            // Offline / no config yet: fall back to defaults instead of failing loudly.
            AppUpdateConfig(githubRepo = DEFAULT_REPO)
        }
    }

    suspend fun saveConfig(config: AppUpdateConfig) {
        db.collection(CONFIG_COLLECTION).document(CONFIG_DOC).set(
            hashMapOf(
                "enabled" to config.enabled,
                "github_repo" to config.githubRepo.trim(),
                "override_download_url" to (config.overrideDownloadUrl?.trim() ?: ""),
                "override_version_name" to (config.overrideVersionName?.trim() ?: ""),
                "release_notes_override" to (config.releaseNotesOverride?.trim() ?: ""),
                "min_version_code" to config.minVersionCode,
                "updated_at" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // ------------------------------------------------------------------
    // Checking GitHub
    // ------------------------------------------------------------------

    /** Returns update info if a newer (or admin-forced) version is available, else null. */
    suspend fun checkForUpdate(currentVersionCode: Int, currentVersionName: String): AppUpdateInfo? =
        withContext(Dispatchers.IO) {
            val config = fetchConfig()
            if (!config.enabled) return@withContext null
            val repo = config.githubRepo.trim().trim('/')
            if (repo.isBlank()) return@withContext null

            val release = fetchLatestRelease(repo)
            // This project's release workflow tags releases as "v<versionName>-<versionCode>"
            // (see .github/workflows/android-release.yml), e.g. "v2.0.0-42". Split those apart
            // so "42" is compared as the actual build number, not as a 4th version segment —
            // treating it as part of the version text made the app think an update was always
            // available, even right after installing that exact release.
            val parsedVersionName: String?
            val parsedVersionCode: Int?
            if (release != null) {
                val parsed = parseTag(release.tagName)
                parsedVersionName = parsed.first
                parsedVersionCode = parsed.second
            } else {
                parsedVersionName = null
                parsedVersionCode = null
            }

            val remoteVersionName = config.overrideVersionName ?: parsedVersionName
            if (remoteVersionName.isNullOrBlank()) return@withContext null

            val mandatory = config.minVersionCode > 0 && currentVersionCode < config.minVersionCode

            val newer = when {
                // Admin forced a specific version label: nothing numeric to trust, compare as text.
                config.overrideVersionName != null -> isVersionNewer(remoteVersionName, currentVersionName)
                // Normal case: the tag carries a real build number, so compare it directly against
                // the installed versionCode. This is exact — no guessing from a version string.
                parsedVersionCode != null -> currentVersionCode < parsedVersionCode
                // Fallback for a differently-named tag with no numeric build suffix.
                else -> isVersionNewer(remoteVersionName, currentVersionName)
            }
            if (!newer && !mandatory) return@withContext null

            val downloadUrl = config.overrideDownloadUrl
                ?: release?.apkUrl
                ?: release?.htmlUrl
                ?: return@withContext null

            AppUpdateInfo(
                versionName = remoteVersionName,
                releaseNotes = config.releaseNotesOverride ?: release?.body?.ifBlank { null } ?: "إصلاحات وتحسينات عامة.",
                downloadUrl = downloadUrl,
                releasePageUrl = release?.htmlUrl ?: "https://github.com/$repo/releases",
                mandatory = mandatory
            )
        }

    /**
     * Splits a release tag into (versionName, versionCode).
     * "v2.0.0-42" -> ("2.0.0", 42). "v2.0.0" (no numeric build suffix) -> ("2.0.0", null).
     */
    private fun parseTag(tag: String): Pair<String, Int?> {
        val withoutV = tag.trim().removePrefix("v").removePrefix("V")
        val lastDash = withoutV.lastIndexOf('-')
        if (lastDash != -1) {
            val code = withoutV.substring(lastDash + 1).toIntOrNull()
            if (code != null) return withoutV.substring(0, lastDash) to code
        }
        return withoutV to null
    }

    private data class ReleaseData(val tagName: String, val body: String, val htmlUrl: String, val apkUrl: String?)

    private fun fetchLatestRelease(repo: String): ReleaseData? = try {
        val url = URL("https://api.github.com/repos/$repo/releases/latest")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        val code = conn.responseCode
        if (code !in 200..299) {
            conn.disconnect()
            null
        } else {
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val json = JSONObject(text)
            val tag = json.optString("tag_name")
            val body = json.optString("body")
            val htmlUrl = json.optString("html_url")
            var apkUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
            }
            if (tag.isBlank()) null else ReleaseData(tag, body, htmlUrl, apkUrl)
        }
    } catch (e: Exception) {
        null
    }

    /** Simple numeric version comparison: 2.10.1 > 2.9.9, 2.1 > 2.0.9, etc. */
    private fun isVersionNewer(remote: String, current: String): Boolean {
        fun parts(v: String) = v.trim().trimStart('v', 'V').split('.', '-')
            .map { seg -> seg.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
        val r = parts(remote)
        val c = parts(current)
        for (i in 0 until maxOf(r.size, c.size)) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv != cv) return rv > cv
        }
        return false
    }

    // ------------------------------------------------------------------
    // Download & install
    // ------------------------------------------------------------------

    data class DownloadStatus(val status: Int, val downloadedBytes: Long, val totalBytes: Long)

    /** Starts the download with the system DownloadManager and returns its download id. */
    fun startDownload(context: Context, info: AppUpdateInfo): Long {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val safeVersion = info.versionName.replace(Regex("[^A-Za-z0-9.]"), "_")
        val fileName = "harbs-update-$safeVersion.apk"
        val request = DownloadManager.Request(Uri.parse(info.downloadUrl))
            .setTitle("تحديث موسوعة الأعشاب")
            .setDescription("جاري تنزيل الإصدار ${info.versionName}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setMimeType("application/vnd.android.package-archive")
        return manager.enqueue(request)
    }

    fun queryStatus(context: Context, downloadId: Long): DownloadStatus {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = manager.query(DownloadManager.Query().setFilterById(downloadId))
        cursor.use {
            if (it.moveToFirst()) {
                val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val downloaded = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                return DownloadStatus(status, downloaded, total)
            }
        }
        return DownloadStatus(DownloadManager.STATUS_FAILED, 0, 0)
    }

    /** True if the app is currently allowed to install the APK it downloaded itself. */
    fun canInstallPackages(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true

    /** Sends the user to the "install unknown apps" system settings screen for this app. */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun installApk(context: Context, downloadId: Long) {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = manager.getUriForDownloadedFile(downloadId) ?: return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
