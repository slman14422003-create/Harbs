package com.salman.herbalencyclopedia.data.repository

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
 * Reads the admin update metadata and checks the configured GitHub Release.
 * The app is distributed as a direct APK download rather than through Google
 * Play, so an available update points straight at the .apk asset's download
 * URL (falling back to the release page if no .apk asset is attached).
 */
class UpdateRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val DEFAULT_REPO = "slman14422003-create/Harbs"
        private const val CONFIG_COLLECTION = "app_config"
        private const val CONFIG_DOC = "update"

        /**
         * Public GitHub mirrors, tried in order, only when direct access to
         * GitHub fails outright (the usual symptom when GitHub is blocked at
         * the country level). Each one works as a plain URL prefix: you paste
         * the *original* github.com / api.github.com / githubusercontent.com
         * URL right after the base and it proxies the request through.
         *
         * These are community-run and can go up/down independently of this
         * app, which is exactly why several are tried in sequence instead of
         * hardcoding just one. An admin can also set a private proxy
         * (customProxyBaseUrl in Firestore) that is tried before this list.
         */
        private val PUBLIC_PROXY_MIRRORS = listOf(
            "https://ghfast.top/",
            "https://gh-proxy.com/",
            "https://ghproxy.net/",
            "https://mirror.ghproxy.com/"
        )
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
                    overrideVersionName = doc.getString("override_version_name")?.trim()?.takeIf { it.isNotBlank() },
                    releaseNotesOverride = doc.getString("release_notes_override")?.trim()?.takeIf { it.isNotBlank() },
                    minVersionCode = (doc.getLong("min_version_code") ?: 0L).toInt(),
                    useProxyFallback = doc.getBoolean("use_proxy_fallback") ?: true,
                    customProxyBaseUrl = doc.getString("custom_proxy_base_url")?.trim()?.takeIf { it.isNotBlank() },
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
                "override_version_name" to (config.overrideVersionName?.trim() ?: ""),
                "release_notes_override" to (config.releaseNotesOverride?.trim() ?: ""),
                "min_version_code" to config.minVersionCode,
                "use_proxy_fallback" to config.useProxyFallback,
                "custom_proxy_base_url" to (config.customProxyBaseUrl?.trim() ?: ""),
                "updated_at" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // ------------------------------------------------------------------
    // Checking GitHub
    // ------------------------------------------------------------------

    /** Returns update info if a newer (or admin-forced) version is available, else null. */
    suspend fun checkForUpdate(currentVersionCode: Int, currentVersionName: String): AppUpdateInfo? =
        checkForUpdate(fetchConfig(), currentVersionCode, currentVersionName)

    /**
     * نفس منطق [checkForUpdate] أعلاه، لكن يأخذ الإعدادات كمعامل مباشر بدل
     * قراءتها من Firestore — يسمح للوحة الإدارة باختبار إعدادات لم تُحفَظ
     * بعد (القيم في الحقول حالياً) فوراً، دون الحاجة لحفظها أولاً ثم الخروج
     * لشاشة المستخدم العادية للتأكد من أنها تعمل كما يُتوقَّع.
     */
    suspend fun checkForUpdate(
        config: AppUpdateConfig,
        currentVersionCode: Int,
        currentVersionName: String
    ): AppUpdateInfo? = withContext(Dispatchers.IO) {
            if (!config.enabled) return@withContext null
            val repo = config.githubRepo.trim().trim('/')
            if (repo.isBlank()) return@withContext null

            val release = fetchLatestReleaseWithFallback(repo, config)
            if (release?.apkUrl == null) return@withContext null
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

            // Direct-download distribution: point the user at the .apk asset itself
            // (falling back to the release page if this release has no .apk attached).
            val releasePageUrl = release?.htmlUrl ?: "https://github.com/$repo/releases"

            AppUpdateInfo(
                versionName = remoteVersionName,
                // Don't show the raw GitHub release body (CI-generated / commit-log style
                // text) to end users — always show a fixed, friendly Arabic message unless
                // the admin explicitly typed a custom one in the admin panel.
                releaseNotes = config.releaseNotesOverride ?: "تم تحديث الأخطاء وإدخال تحسينات جديدة.",
                releasePageUrl = releasePageUrl,
                apkUrl = release?.apkUrl,
                mandatory = mandatory,
                useProxyFallback = config.useProxyFallback,
                customProxyBaseUrl = config.customProxyBaseUrl
            )
        }

    /**
     * Ordered list of URLs to try when actually downloading [originalUrl] (a plain,
     * unproxied github.com / objects.githubusercontent.com link): the direct link
     * first, then each configured proxy mirror in turn — the exact same fallback
     * chain [fetchLatestReleaseWithFallback] uses for the metadata check, but kept
     * independent because the asset CDN can be reachable/blocked differently than
     * the metadata API. Called from [com.salman.herbalencyclopedia.ui.AppViewModel]
     * so the in-app .apk download gets its own retry chain instead of only reusing
     * whichever path the earlier metadata check happened to succeed through.
     */
    fun downloadCandidates(originalUrl: String, useProxyFallback: Boolean, customProxyBaseUrl: String?): List<String> {
        if (!useProxyFallback) return listOf(originalUrl)
        val proxies = buildList {
            customProxyBaseUrl?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
            addAll(PUBLIC_PROXY_MIRRORS)
        }
        return listOf(originalUrl) + proxies.map { viaProxy(it, originalUrl) }
    }

    /**
     * Tries GitHub directly first. If that fails outright (connection refused,
     * timeout, DNS failure — the pattern seen when GitHub is blocked at the
     * network/country level) it retries the exact same request through a
     * proxy mirror, so the update check keeps working without the user
     * needing a VPN.
     *
     * The public mirrors in [PUBLIC_PROXY_MIRRORS] are built and mainly used
     * for proxying plain github.com / objects.githubusercontent.com *file*
     * requests (releases, raw files) — not necessarily the api.github.com
     * REST API this method calls for metadata. In practice this means a
     * network that blocks GitHub can still leave the *version check* stuck
     * (silently requiring a VPN) even though the same mirrors would happily
     * serve the actual .apk afterward. So when every api.github.com attempt
     * (direct + every proxy) fails, [fetchLatestReleaseViaHtmlFallback] is
     * tried as a last resort: it never touches api.github.com at all, only
     * plain github.com pages, which these mirrors are actually built for.
     */
    private fun fetchLatestReleaseWithFallback(repo: String, config: AppUpdateConfig): ReleaseData? {
        val direct = fetchLatestRelease(repo)
        if (direct != null) return direct
        if (!config.useProxyFallback) return null

        val proxies = buildList {
            config.customProxyBaseUrl?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
            addAll(PUBLIC_PROXY_MIRRORS)
        }
        for (proxyBase in proxies) {
            val result = fetchLatestRelease(repo, proxyBase)
            if (result != null) return result
        }
        return fetchLatestReleaseViaHtmlFallback(repo, config)
    }

    /** Rewrites a github.com/objects.githubusercontent.com URL to go through a proxy prefix. */
    private fun viaProxy(proxyBase: String, url: String): String =
        proxyBase.trimEnd('/') + "/" + url

    /**
     * Last-resort metadata fetch that avoids api.github.com entirely: it
     * follows GitHub's ordinary "latest release" redirect (a plain github.com
     * URL) to discover the release tag, then scans that release page's HTML
     * for the .apk asset's download link — both are requests the public proxy
     * mirrors are actually designed to handle, unlike the JSON API above.
     * Tried direct first, then through the same proxy chain, since the HTML
     * page can be blocked independently of the API too.
     */
    private fun fetchLatestReleaseViaHtmlFallback(repo: String, config: AppUpdateConfig): ReleaseData? {
        val bases = buildList<String?> {
            add(null) // مباشر بلا بروكسي
            config.customProxyBaseUrl?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
            addAll(PUBLIC_PROXY_MIRRORS)
        }
        for (proxyBase in bases) {
            val result = runCatching { fetchLatestReleaseViaHtml(repo, proxyBase) }.getOrNull()
            if (result != null) return result
        }
        return null
    }

    private fun fetchLatestReleaseViaHtml(repo: String, proxyBase: String?): ReleaseData? {
        val latestUrl = "https://github.com/$repo/releases/latest"
        val requestUrl = if (proxyBase != null) viaProxy(proxyBase, latestUrl) else latestUrl
        // "releases/latest" هو رابط إعادة توجيه دائم من GitHub نحو
        // ".../releases/tag/<tag>" — لا نتابعه تلقائياً، بل نقرأ رأس
        // Location مباشرة لاستخراج اسم التاج دون تحميل أي صفحة إضافية.
        val location = resolveRedirectLocation(requestUrl) ?: return null
        val tag = location.trimEnd('/').substringAfterLast('/').substringBefore('?')
        if (tag.isBlank()) return null

        val tagPageUrl = "https://github.com/$repo/releases/tag/$tag"
        val requestTagUrl = if (proxyBase != null) viaProxy(proxyBase, tagPageUrl) else tagPageUrl
        val html = fetchText(requestTagUrl) ?: return null

        val apkPath = Regex("""href="(/${Regex.escape(repo)}/releases/download/[^"]+?\.apk)"""")
            .find(html)?.groupValues?.get(1)
        val apkUrl = apkPath?.replace("&amp;", "&")?.let { "https://github.com$it" }

        return ReleaseData(tagName = tag, body = "", htmlUrl = tagPageUrl, apkUrl = apkUrl)
    }

    /** يفتح الرابط بلا اتّباع تحويل تلقائي، ويُرجع رأس Location الخام إن كان رد التحويل (3xx). */
    private fun resolveRedirectLocation(url: String): String? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", "Harbs-App-Update-Checker")
        }
        return try {
            val code = conn.responseCode
            if (code in 300..399) conn.getHeaderField("Location") else null
        } finally {
            conn.disconnect()
        }
    }

    /** يجلب نص صفحة عادية (مع اتّباع أي تحويلات) — يُستخدم لقراءة HTML صفحة الإصدار فقط. */
    private fun fetchText(url: String): String? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Harbs-App-Update-Checker")
        }
        return try {
            val code = conn.responseCode
            if (code !in 200..299) null else conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
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

    private data class ReleaseData(
        val tagName: String,
        val body: String,
        /** Always the plain, unproxied github.com URL — never rewritten, even when this
         *  JSON itself was fetched through a proxy mirror. See [downloadCandidates]. */
        val htmlUrl: String,
        /** Always the plain, unproxied asset URL, for the same reason. */
        val apkUrl: String?
    )

    /**
     * Fetches the latest release JSON from GitHub's API. When [proxyBase] is
     * given, the api.github.com request itself is routed through the proxy
     * (some mirrors only proxy github.com/objects.githubusercontent.com, so a
     * connection failure here is treated the same as any other failure — the
     * caller just moves on to the next mirror). The asset/release-page links
     * inside the JSON body are themselves already plain github.com URLs
     * regardless of how we fetched the JSON, so they're returned as-is —
     * proxying them (if needed at all) happens uniformly later via
     * [downloadCandidates], instead of guessing here whether the same proxy
     * that worked for the metadata API will also work for the asset CDN.
     */
    private fun fetchLatestRelease(repo: String, proxyBase: String? = null): ReleaseData? = try {
        val apiUrl = "https://api.github.com/repos/$repo/releases/latest"
        val requestUrl = if (proxyBase != null) viaProxy(proxyBase, apiUrl) else apiUrl
        val conn = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Harbs-App-Update-Checker")
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
            // apkUrl/htmlUrl are left as plain github.com URLs here regardless of
            // [proxyBase] — see the ReleaseData/downloadCandidates doc comments.
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


}
