package com.example.artsan_finder.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.artsan_finder.data.model.Artisan
import com.example.artsan_finder.data.model.User
import com.example.artsan_finder.data.repository.ArtisanRepository
import com.example.artsan_finder.data.repository.AuthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AdminUiState(
    val adminUser: com.example.artsan_finder.data.model.User? = null,
    val nidaProfile: com.example.artsan_finder.data.model.NidaProfile? = null,
    val isVerifyingNida: Boolean = false,
    val nidaError: String? = null,
    val stats: Map<String, Int> = emptyMap(),
    val pendingArtisans: List<Artisan> = emptyList(),
    val allArtisans: List<Artisan> = emptyList(),
    val allCustomers: List<com.example.artsan_finder.data.model.User> = emptyList(),
    val categories: List<com.example.artsan_finder.data.model.Category> = emptyList(),
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isGeocoding: Boolean = false,
    val previewCoordinates: Pair<Double, Double>? = null,
    val previewFailed: Boolean = false,
    // One-shot feedback shown to the admin (e.g. "reset email sent"), cleared after display
    val message: String? = null
)

class AdminViewModel(
    private val adminId: String,
    private val repository: ArtisanRepository,
    private val authRepository: AuthRepository,
    private val nidaService: com.example.artsan_finder.data.remote.NidaVerificationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        observeDashboardData()
        observeStats()
        viewModelScope.launch {
            // Give seeded demo artisans real login accounts and user profiles
            val provisioned = repository.provisionDemoArtisanLogins()
            repository.backfillArtisanUserProfiles()
            if (provisioned > 0) {
                _uiState.update {
                    it.copy(message = "$provisioned demo artisan login(s) created — password: ${ArtisanRepository.DEMO_ARTISAN_PASSWORD}")
                }
            }
        }
    }

    /** Counters update in real time from the same listeners that feed the lists. */
    private fun observeStats() {
        viewModelScope.launch {
            repository.adminStats.collect { stats ->
                _uiState.update { it.copy(stats = stats) }
            }
        }
    }

    /**
     * Single long-lived collector: Firestore snapshot listeners push every change
     * in real time, so this is started once and never restarted. Stats are fetched
     * separately so list data renders immediately without waiting on counts.
     */
    private fun observeDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                repository.artisans,
                repository.categories,
                repository.getAllCustomers(),
                repository.getUserById(adminId)
            ) { artisans, categories, customers, admin ->
                val pending = artisans.filter { !it.verified }
                _uiState.update {
                    it.copy(
                        adminUser = admin,
                        pendingArtisans = pending,
                        allArtisans = artisans,
                        allCustomers = customers,
                        categories = categories,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    /**
     * Updates the admin's profile in Firestore immediately, and optionally rotates
     * their own login credentials. Changing the login email or password requires
     * the current password (Firebase re-authentication).
     */
    fun updateAdminProfile(
        name: String,
        email: String,
        currentPassword: String = "",
        newPassword: String = ""
    ) {
        viewModelScope.launch {
            val currentUser = _uiState.value.adminUser ?: return@launch
            _uiState.update { it.copy(isLoading = true) }

            val feedback = mutableListOf<String>()

            // 1. Profile document — instant, real-time to all devices
            val profileResult = repository.updateUserInfo(currentUser.copy(name = name, email = email))
            profileResult.onFailure { e ->
                feedback += e.message ?: "Could not update profile"
            }

            // 2. Login password (own account only — requires re-authentication)
            if (newPassword.isNotBlank()) {
                if (currentPassword.isBlank()) {
                    feedback += "Enter your current password to change the login password"
                } else {
                    authRepository.changeOwnPassword(currentPassword, newPassword)
                        .onSuccess { feedback += "Login password updated" }
                        .onFailure { e -> feedback += e.message ?: "Password change failed" }
                }
            }

            // 3. Login email (own account only — verified via link to the new address)
            if (!email.trim().equals(currentUser.email, ignoreCase = true)) {
                if (currentPassword.isBlank()) {
                    feedback += "Enter your current password to change the login email"
                } else {
                    authRepository.changeOwnLoginEmail(currentPassword, email)
                        .onSuccess { msg -> feedback += msg }
                        .onFailure { e -> feedback += e.message ?: "Login email change failed" }
                }
            }

            _uiState.update {
                it.copy(
                    isSuccess = profileResult.isSuccess,
                    isLoading = false,
                    message = feedback.joinToString("\n").ifBlank { "Profile updated" }
                )
            }
        }
    }

    /** Admin sets a new login email and/or password for an artisan account. */
    fun changeArtisanLogin(
        artisanId: String,
        currentPassword: String,
        newEmail: String,
        newPassword: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.changeArtisanLogin(artisanId, currentPassword, newEmail, newPassword)
                .onSuccess { msg -> _uiState.update { it.copy(isLoading = false, message = msg) } }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, message = e.message ?: "Could not change login credentials")
                    }
                }
        }
    }

    /** Emails the user a secure Firebase link to set a new password themselves. */
    fun sendPasswordReset(userId: String) {
        viewModelScope.launch {
            repository.sendPasswordResetEmail(userId)
                .onSuccess { email ->
                    // Firebase hides whether an Auth account exists (seeded demo profiles have
                    // none), so the request can "succeed" without any email being delivered.
                    _uiState.update { it.copy(message = "Reset link requested for $email — it arrives only if that address has a real login (check spam too)") }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(message = e.message ?: "Could not send reset email") }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun approveArtisan(artisanId: String) {
        viewModelScope.launch {
            repository.verifyArtisan(artisanId, true)
        }
    }

    fun rejectArtisan(artisanId: String) {
        viewModelScope.launch {
            repository.verifyArtisan(artisanId, false)
        }
    }

    fun registerArtisan(
        name: String,
        categoryId: String,
        region: String,
        ward: String,
        street: String,
        phoneNumber: String,
        email: String,
        password: String,
        bio: String,
        imageUrl: String,
        isVerified: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isSuccess = false) }
            repository.registerArtisan(
                name, categoryId, region, ward, street, phoneNumber, email, password, bio, imageUrl, isVerified
            ).onSuccess {
                _uiState.update { it.copy(isSuccess = true, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, isSuccess = false) }
            }
        }
    }

    fun updateArtisan(
        artisan: Artisan,
        region: String? = null,
        ward: String? = null,
        street: String? = null
    ) {
        viewModelScope.launch {
            repository.updateArtisanProfile(artisan, region, ward, street).onSuccess {
                _uiState.update { it.copy(isSuccess = true) }
            }
        }
    }

    fun deleteArtisan(artisanId: String) {
        viewModelScope.launch {
            repository.deleteArtisan(artisanId).onSuccess {
                _uiState.update { it.copy(isSuccess = true) }
            }
        }
    }

    fun updateCustomer(customer: User) {
        viewModelScope.launch {
            repository.updateUserInfo(customer).onSuccess {
                _uiState.update { it.copy(isSuccess = true) }
            }
        }
    }

    fun deleteCustomer(userId: String) {
        viewModelScope.launch {
            repository.deleteCustomer(userId).onSuccess {
                _uiState.update { it.copy(isSuccess = true) }
            }
        }
    }

    fun resetSuccessState() {
        _uiState.update { it.copy(isSuccess = false) }
    }

    fun verifyNida(nidaNumber: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isVerifyingNida = true, nidaError = null, nidaProfile = null) }
            nidaService.verifyNida(nidaNumber)
                .onSuccess { profile ->
                    _uiState.update { it.copy(nidaProfile = profile, isVerifyingNida = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(nidaError = e.message, isVerifyingNida = false) }
                }
        }
    }

    fun clearNidaData() {
        _uiState.update { it.copy(nidaProfile = null, nidaError = null, isVerifyingNida = false) }
    }

    /**
     * Geocodes the entered address so the admin can verify the exact pin
     * on a map before saving the artisan's new location.
     */
    fun previewArtisanLocation(region: String, ward: String, street: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeocoding = true, previewCoordinates = null, previewFailed = false) }
            val coords = repository.geocodeAddress(region, ward, street)
            _uiState.update {
                it.copy(
                    isGeocoding = false,
                    previewCoordinates = coords,
                    previewFailed = coords == null
                )
            }
        }
    }

    fun clearLocationPreview() {
        _uiState.update { it.copy(isGeocoding = false, previewCoordinates = null, previewFailed = false) }
    }

    companion object {
        fun provideFactory(
            adminId: String,
            repository: ArtisanRepository,
            authRepository: AuthRepository,
            nidaService: com.example.artsan_finder.data.remote.NidaVerificationService
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AdminViewModel(adminId, repository, authRepository, nidaService) as T
            }
        }
    }
}
