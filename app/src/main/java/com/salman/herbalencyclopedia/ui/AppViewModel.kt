package com.salman.herbalencyclopedia.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salman.herbalencyclopedia.data.model.Category
import com.salman.herbalencyclopedia.data.model.Herb
import com.salman.herbalencyclopedia.data.repository.AppContainer
import com.salman.herbalencyclopedia.data.repository.HerbRepository
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

class AppViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val favoriteIds: StateFlow<Set<String>> = container.preferencesRepository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    var isLoggedIn by mutableStateOf(container.authRepository.isAdmin)
        private set
    var isAdmin by mutableStateOf(container.authRepository.isAdmin)
        private set

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
