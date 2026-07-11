package com.example.artsan_finder.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.artsan_finder.data.model.User
import com.example.artsan_finder.data.repository.ArtisanRepository
import com.example.artsan_finder.data.repository.AuthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfileUiState(
    /** Live profile from the users collection — updates in real time. */
    val user: User? = null,
    /** The address the user logs in with, owned by Firebase Auth. */
    val loginEmail: String = "",
    /** Firebase Auth account creation time — the registration date. */
    val memberSince: Long? = null,
    val isWorking: Boolean = false,
    /** One-shot feedback (success or failure) surfaced as a snackbar. */
    val message: String? = null
)

class ProfileViewModel(
    private val userId: String,
    private val authRepository: AuthRepository,
    private val artisanRepository: ArtisanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            loginEmail = authRepository.currentLoginEmail.orEmpty(),
            memberSince = authRepository.accountCreatedAt
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        // Live profile doc: a name edited here (or by an admin) is reflected
        // immediately, and every other screen showing the name follows suit.
        viewModelScope.launch {
            authRepository.getUser(userId).collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    fun updateName(newName: String) {
        val user = _uiState.value.user ?: return
        val trimmed = newName.trim()
        if (trimmed.isBlank() || trimmed == user.name) return
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true) }
            artisanRepository.updateUserInfo(user.copy(name = trimmed))
                .onSuccess {
                    _uiState.update { it.copy(isWorking = false, message = "Name updated") }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isWorking = false, message = e.message ?: "Could not update the name")
                    }
                }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true) }
            authRepository.changeOwnPassword(currentPassword, newPassword)
                .onSuccess {
                    _uiState.update { it.copy(isWorking = false, message = "Password changed successfully") }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isWorking = false, message = e.message ?: "Could not change the password")
                    }
                }
        }
    }

    fun changeLoginEmail(currentPassword: String, newEmail: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true) }
            authRepository.changeOwnLoginEmail(currentPassword, newEmail)
                .onSuccess { info ->
                    _uiState.update { it.copy(isWorking = false, message = info) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isWorking = false, message = e.message ?: "Could not change the login email")
                    }
                }
        }
    }

    /** Deletes profile + login. [onDeleted] runs after so the caller can sign out and navigate. */
    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true) }
            authRepository.deleteAccount(userId)
            onDeleted()
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    companion object {
        fun provideFactory(
            userId: String,
            authRepository: AuthRepository,
            artisanRepository: ArtisanRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(userId, authRepository, artisanRepository) as T
            }
        }
    }
}
