package com.example.artsan_finder.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.artsan_finder.data.model.User
import com.example.artsan_finder.data.model.UserRole
import com.example.artsan_finder.data.repository.AuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AuthNavigationEvent {
    data class NavigateToDashboard(val role: UserRole) : AuthNavigationEvent()
}

data class AuthUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRegistered: Boolean = false,
    val infoMessage: String? = null,
    val isSendingReset: Boolean = false
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<AuthNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    init {
        // Enforce session persistence rules on startup
        viewModelScope.launch {
            repository.checkSessionPersistence()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: StateFlow<User?> = repository.currentUserId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getUser(id)
                .onStart<User?> { emit(null) }
                .combine(_uiState) { profile, state ->
                    // Until the live profile snapshot delivers, serve the user
                    // login() already fetched — dashboards get the real id, name
                    // and role on their very first frame instead of a null that
                    // makes them boot with placeholder identities and reload.
                    // Guarded by the auth uid so a stale login result can never
                    // impersonate a different (or signed-out) session.
                    profile ?: state.user?.takeIf { it.id == id }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun login(email: String, password: String, remember: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
            repository.login(email, password, remember)
                .onSuccess { user ->
                    _uiState.update { it.copy(user = user, isLoading = false) }
                    _navigationEvent.emit(AuthNavigationEvent.NavigateToDashboard(user.role))
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.register(name, email, password)
                .onSuccess { user ->
                    _uiState.update { it.copy(user = user, isLoading = false, isRegistered = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.update { AuthUiState() }
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingReset = true, error = null, infoMessage = null) }
            repository.sendPasswordResetEmail(email)
                .onSuccess { sentTo ->
                    _uiState.update {
                        it.copy(
                            isSendingReset = false,
                            infoMessage = "Reset link sent to $sentTo successfully"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSendingReset = false, error = e.message) }
                }
        }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun resetRegistrationState() {
        _uiState.update { it.copy(isRegistered = false) }
    }

    fun clearRegistrationForm() {
        _uiState.update { it.copy(user = null, isRegistered = false, error = null, isLoading = false) }
    }

    // Biometric login removed: enableBiometric/disableBiometric/loginWithBiometric removed

    companion object {
        fun provideFactory(repository: AuthRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(repository) as T
            }
        }
    }
}
