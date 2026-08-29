package com.salman.herbalencyclopedia.ui

import android.app.DownloadManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salman.herbalencyclopedia.data.model.AppUpdateConfig
import com.salman.herbalencyclopedia.data.model.AppUpdateInfo
import com.salman.herbalencyclopedia.data.model.Category
import com.salman.herbalencyclopedia.data.model.Herb
import com.salman.herbalencyclopedia.data.repository.AppContainer
import com.salman.herbalencyclopedia.data.repository.HerbRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

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

/** State of the APK download that follows a detected update (see [AppViewModel.downloadUpdate]). */
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
    // In-app updates (check GitHub Release -> download -> install)
    // ---------------------------------------------------------------------

    private val _updateState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateState: StateFlow<UpdateCheckState> = _updateState.asStateFlow()

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    private val _updateConfig = MutableStateFlow(AppUpdateConfig())
    val updateConfigState: StateFlow<AppUpdateConfig> = _updateConfig.asStateFlow()

    private var activeDownloadId: Long? = null

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

    /** Downloads the update APK with the system DownloadManager, tracking progress until it's ready to install. */
    fun downloadUpdate(context: Context, info: AppUpdateInfo) {
        viewModelScope.launch {
            _downloadState.value = UpdateDownloadState.Downloading(0)
            val id = try {
                container.updateRepository.startDownload(context, info)
            } catch (e: Exception) {
                _downloadState.value = UpdateDownloadState.Failed(e.localizedMessage ?: "تعذّر بدء التنزيل")
                return@launch
            }
            activeDownloadId = id
            while (true) {
                val status = container.updateRepository.queryStatus(context, id)
                when (status.status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        _downloadState.value = UpdateDownloadState.ReadyToInstall
                        return@launch
                    }
                    DownloadManager.STATUS_FAILED -> {
                        _downloadState.value = UpdateDownloadState.Failed("فشل تنزيل التحديث، تحقق من الاتصال وحاول مرة أخرى")
                        return@launch
                    }
                    else -> {
                        val pct = if (status.totalBytes > 0) ((status.downloadedBytes * 100) / status.totalBytes).toInt() else 0
                        _downloadState.value = UpdateDownloadState.Downloading(pct.coerceIn(0, 100))
                    }
                }
                delay(400)
            }
        }
    }

    /** Launches the package installer for the already-downloaded update, asking for install permission first if needed. */
    fun installUpdate(context: Context) {
        val id = activeDownloadId ?: return
        if (!container.updateRepository.canInstallPackages(context)) {
            container.updateRepository.openInstallPermissionSettings(context)
            return
        }
        container.updateRepository.installApk(context, id)
    }

    /** Resets the update flow back to its initial state (e.g. after a dismissal). */
    fun resetUpdateFlow() {
        _updateState.value = UpdateCheckState.Idle
        _downloadState.value = UpdateDownloadState.Idle
        activeDownloadId = null
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
