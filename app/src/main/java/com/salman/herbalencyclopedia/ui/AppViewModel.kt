package com.salman.herbalencyclopedia.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salman.herbalencyclopedia.data.model.AppUpdateConfig
import com.salman.herbalencyclopedia.data.model.AppUpdateInfo
import com.salman.herbalencyclopedia.data.model.Category
import com.salman.herbalencyclopedia.data.model.Herb
import com.salman.herbalencyclopedia.data.repository.AppContainer
import com.salman.herbalencyclopedia.data.repository.HerbRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UiState(
    val herbs: List<Herb> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/** State of an in-app "check for updates" action (see [AppViewModel.checkForUpdate]). */
sealed class UpdateCheckState {
    data object Idle : UpdateCheckState()
    data object Checking : UpdateCheckState()
    data object UpToDate : UpdateCheckState()
    data class Available(val info: AppUpdateInfo) : UpdateCheckState()
    data class Error(val message: String) : UpdateCheckState()
}

/** State of the direct-APK-download update hand-off (see [AppViewModel.downloadUpdate]). */
sealed class UpdateDownloadState {
    data object Idle : UpdateDownloadState()
    data class Downloading(val progress: Int) : UpdateDownloadState()
    data object ReadyToInstall : UpdateDownloadState()
    data class Failed(val message: String) : UpdateDownloadState()
}

class AppViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val favoriteIds: StateFlow<Set<String>> = container.preferencesRepository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    var isLoggedIn by mutableStateOf(container.authRepository.isAdmin)
        private set
    var isAdmin by mutableStateOf(container.authRepository.isAdmin)
        private set

    // ---------------------------------------------------------------------
    // Update check -> direct APK download hand-off
    // ---------------------------------------------------------------------

    private val _updateState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateState: StateFlow<UpdateCheckState> = _updateState.asStateFlow()

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    private val _updateConfig = MutableStateFlow(AppUpdateConfig())
    val updateConfigState: StateFlow<AppUpdateConfig> = _updateConfig.asStateFlow()


    /** Reads the app's own installed version and checks the configured GitHub repo for a newer release. */
    fun checkForUpdate(context: Context) {
        if (_updateState.value == UpdateCheckState.Checking) return
        viewModelScope.launch {
            _updateState.value = UpdateCheckState.Checking
            val pkgInfo = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }.getOrNull()
            val versionName = pkgInfo?.versionName ?: "0.0.0"
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) {
                (pkgInfo?.longVersionCode ?: 0L).toInt()
            } else {
                @Suppress("DEPRECATION") (pkgInfo?.versionCode ?: 0)
            }
            val result = runCatching { container.updateRepository.checkForUpdate(versionCode, versionName) }
            _updateState.value = result.fold(
                onSuccess = { info -> if (info != null) UpdateCheckState.Available(info) else UpdateCheckState.UpToDate },
                onFailure = { e -> UpdateCheckState.Error(e.localizedMessage ?: "تعذّر التحقق من التحديثات") }
            )
        }
    }

    // Remembers the last update info so installUpdate() (which isn't passed the
    // info again from the UI) can re-open the same download link, e.g. on retry.
    private var lastUpdateInfo: AppUpdateInfo? = null

    // The .apk file downloadUpdate() saved into the app's private cache dir,
    // used by installUpdate() to hand off to the system package installer.
    private var lastDownloadedApk: File? = null

    /**
     * Downloads the .apk asset in-app (into the private cache dir), reporting progress via
     * [downloadState] so the settings screen can show the usual progress bar — instead of
     * sending the user out to the browser to download it themselves.
     *
     * If this release has no .apk asset attached (only a release page), there's nothing to
     * download in-app, so we fall back to opening that page in the browser as before.
     */
    fun downloadUpdate(context: Context, info: AppUpdateInfo) {
        lastUpdateInfo = info
        val apkUrl = info.apkUrl
        if (apkUrl == null) {
            val opened = openInBrowser(context, info.releasePageUrl)
            _downloadState.value = if (opened) {
                UpdateDownloadState.ReadyToInstall
            } else {
                UpdateDownloadState.Failed("تعذّر فتح رابط التحميل")
            }
            return
        }

        val appContext = context.applicationContext
        _downloadState.value = UpdateDownloadState.Downloading(0)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                downloadApkToCache(appContext, apkUrl, info.versionName)
            }
            _downloadState.value = result.fold(
                onSuccess = { file ->
                    lastDownloadedApk = file
                    UpdateDownloadState.ReadyToInstall
                },
                onFailure = { e -> UpdateDownloadState.Failed(e.localizedMessage ?: "فشل تحميل التحديث") }
            )
        }
    }

    /** Streams the .apk to context.cacheDir/updates, publishing percent progress to [downloadState]. */
    private fun downloadApkToCache(context: Context, url: String, versionName: String): Result<File> = runCatching {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        // Drop any previous downloaded update(s) so the cache doesn't grow unbounded.
        dir.listFiles()?.forEach { it.delete() }
        val destFile = File(dir, "update-$versionName.apk")

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 20000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Harbs-App-Update-Downloader")
        }
        val code = conn.responseCode
        if (code !in 200..299) {
            conn.disconnect()
            error("فشل الاتصال بالخادم (رمز $code)")
        }

        val totalSize = conn.contentLength
        var lastReportedPercent = -1
        conn.inputStream.use { input ->
            FileOutputStream(destFile).use { output ->
                val buffer = ByteArray(8 * 1024)
                var totalRead = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    totalRead += read
                    if (totalSize > 0) {
                        val percent = ((totalRead * 100) / totalSize).toInt().coerceIn(0, 100)
                        if (percent != lastReportedPercent) {
                            lastReportedPercent = percent
                            _downloadState.value = UpdateDownloadState.Downloading(percent)
                        }
                    }
                }
            }
        }
        conn.disconnect()
        destFile
    }

    /**
     * Hands the already-downloaded .apk to the system package installer via a FileProvider
     * content:// Uri. On Android 8+ this also makes sure "install unknown apps" is allowed for
     * this app first — if not, it opens that settings screen and the user just taps the
     * install button again once they've granted it.
     */
    fun installUpdate(context: Context) {
        val file = lastDownloadedApk
        if (file == null || !file.exists()) {
            // Nothing was downloaded in-app (e.g. the release had no .apk asset) - fall back
            // to whatever link we have.
            lastUpdateInfo?.let { openInBrowser(context, it.apkUrl ?: it.releasePageUrl) }
            return
        }
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(settingsIntent) }
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { _downloadState.value = UpdateDownloadState.Failed("تعذّر فتح مثبّت التطبيقات") }
    }

    /** Opens a URL (the GitHub release page, when there's no .apk asset to download in-app) in the browser. */
    private fun openInBrowser(context: Context, url: String): Boolean {
        return runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.isSuccess
    }

    /** Resets the update flow back to its initial state (e.g. after a dismissal). */
    fun resetUpdateFlow() {
        _updateState.value = UpdateCheckState.Idle
        _downloadState.value = UpdateDownloadState.Idle
    }

    /** Loads the current admin-editable update settings, for [AdminUpdateScreen]. */
    fun loadUpdateConfig() {
        viewModelScope.launch {
            _updateConfig.value = runCatching { container.updateRepository.fetchConfig() }
                .getOrDefault(AppUpdateConfig())
        }
    }

    /** Saves admin-editable update settings (GitHub repo, override link/notes, mandatory-update threshold). */
    fun saveUpdateConfig(config: AppUpdateConfig, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            runCatching { container.updateRepository.saveConfig(config) }
                .onSuccess {
                    _updateConfig.value = config
                    onResult(true, "تم حفظ إعدادات التحديث")
                }
                .onFailure { onResult(false, it.localizedMessage ?: "حدث خطأ أثناء الحفظ") }
        }
    }

    init {
        // Live sync: stay subscribed to Firestore for as long as the app is alive, so
        // any change - made here, from another device, or from the web admin panel -
        // is reflected immediately without needing a manual refresh.
        viewModelScope.launch {
            combine(
                container.herbRepository.observeCategories(),
                container.herbRepository.observeHerbs()
            ) { categories, herbs ->
                UiState(herbs = herbs, categories = categories, isLoading = false, error = null)
            }
                .catch { e ->
                    // Keep whatever data is already on screen (e.g. from the offline
                    // cache) and only surface the error, instead of wiping the list.
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = HerbRepository.describeError(e)
                    )
                }
                .collect { state -> _uiState.value = state }
        }
    }

    /** Manual retry: forces a real server round-trip to confirm connectivity and clear any error. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                container.herbRepository.fetchCategories(fromServer = true)
                container.herbRepository.fetchHerbs(fromServer = true)
                // The live listeners above already keep uiState in sync with these
                // results; this call's job is just to confirm connectivity and
                // surface a clear error if it fails.
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = HerbRepository.describeError(e)
                )
            }
        }
    }

    fun toggleFavorite(herbId: String) {
        viewModelScope.launch { container.preferencesRepository.toggleFavorite(herbId) }
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = container.authRepository.login(email, password)
            isLoggedIn = result.success
            isAdmin = result.isAdmin
            onResult(result.success, result.message)
        }
    }


    fun logout() {
        container.authRepository.logout()
        isLoggedIn = false
        isAdmin = false
    }

    // Writes below don't call refresh(): the live Firestore listeners in init{}
    // pick up every change automatically (instantly from the local cache, then
    // reconciled with the server), so an extra manual fetch would just be a
    // redundant round-trip and could momentarily race with the listener.

    fun addHerb(herb: Herb, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                container.herbRepository.addHerb(herb)
                onDone(true, null)
            } catch (e: Exception) {
                onDone(false, HerbRepository.describeError(e))
            }
        }
    }

    fun updateHerb(herb: Herb, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                container.herbRepository.updateHerb(herb)
                onDone(true, null)
            } catch (e: Exception) {
                onDone(false, HerbRepository.describeError(e))
            }
        }
    }

    fun deleteHerb(id: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                container.herbRepository.deleteHerb(id)
                onDone(true, null)
            } catch (e: Exception) {
                onDone(false, HerbRepository.describeError(e))
            }
        }
    }
    fun addCategory(name: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch { runCatching { container.herbRepository.addCategory(name) }.onSuccess { onResult(true, null) }.onFailure { onResult(false, HerbRepository.describeError(it)) } }
    }

    fun deleteCategory(id: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch { runCatching { container.herbRepository.deleteCategory(id) }.onSuccess { onResult(true, null) }.onFailure { onResult(false, HerbRepository.describeError(it)) } }
    }

    fun deleteAllHerbs(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch { runCatching { container.herbRepository.deleteAllHerbs() }.onSuccess { onResult(true, null) }.onFailure { onResult(false, HerbRepository.describeError(it)) } }
    }

    fun deleteAllData(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch { runCatching { container.herbRepository.deleteAllData() }.onSuccess { onResult(true, null) }.onFailure { onResult(false, HerbRepository.describeError(it)) } }
    }

    fun clearFavorites() { viewModelScope.launch { container.preferencesRepository.clearFavorites() } }

    fun restoreBackup(json: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch { runCatching { container.herbRepository.restoreBackup(json) }.onSuccess { onResult(true, null) }.onFailure { onResult(false, HerbRepository.describeError(it)) } }
    }

    fun testConnection(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch { runCatching { container.herbRepository.testConnection() }.onSuccess { onResult(true, "الاتصال يعمل بشكل طبيعي") }.onFailure { onResult(false, HerbRepository.describeError(it)) } }
    }

}
