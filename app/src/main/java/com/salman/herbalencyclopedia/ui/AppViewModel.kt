package com.salman.herbalencyclopedia.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salman.herbalencyclopedia.data.model.Category
import com.salman.herbalencyclopedia.data.model.Herb
import com.salman.herbalencyclopedia.data.repository.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    var isLoggedIn by mutableStateOf(container.authRepository.currentUser != null)
        private set
    var isAdmin by mutableStateOf(container.authRepository.isAdmin)
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val categories = container.herbRepository.fetchCategories()
                val herbs = container.herbRepository.fetchHerbs()
                _uiState.value = UiState(herbs = herbs, categories = categories, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "تعذّر تحميل البيانات"
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

    fun register(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = container.authRepository.register(email, password)
            onResult(result.success, result.message)
        }
    }

    fun logout() {
        container.authRepository.logout()
        isLoggedIn = false
        isAdmin = false
    }

    fun addHerb(herb: Herb, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                container.herbRepository.addHerb(herb)
                refresh()
                onDone(true, null)
            } catch (e: Exception) {
                onDone(false, e.localizedMessage)
            }
        }
    }

    fun updateHerb(herb: Herb, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                container.herbRepository.updateHerb(herb)
                refresh()
                onDone(true, null)
            } catch (e: Exception) {
                onDone(false, e.localizedMessage)
            }
        }
    }

    fun deleteHerb(id: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                container.herbRepository.deleteHerb(id)
                refresh()
                onDone(true, null)
            } catch (e: Exception) {
                onDone(false, e.localizedMessage)
            }
        }
    }
}
