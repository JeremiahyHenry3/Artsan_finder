package com.example.artsan_finder.ui.artisan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.artsan_finder.data.model.Artisan
import com.example.artsan_finder.data.model.Inquiry
import com.example.artsan_finder.data.model.Service
import com.example.artsan_finder.data.repository.ArtisanRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class ArtisanDashboardUiState(
    val artisan: Artisan? = null,
    val inquiries: List<Inquiry> = emptyList(),
    val services: List<Service> = emptyList(),
    /** Customer display names keyed by userId, resolved lazily per inquiry. */
    val customerNames: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isUpdatingLocation: Boolean = false,
    val locationMessage: String? = null
)

class ArtisanViewModel(
    private val artisanId: String,
    private val repository: ArtisanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtisanDashboardUiState())
    val uiState: StateFlow<ArtisanDashboardUiState> = _uiState.asStateFlow()

    // Names already requested — avoids re-fetching on every inquiries emission
    private val requestedNames = mutableSetOf<String>()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Three live snapshot listeners: profile, inquiries and services.
            // Every emission is current cloud state — the dashboard is always real.
            combine(
                repository.observeArtisanById(artisanId),
                repository.getInquiriesForArtisan(artisanId),
                repository.getServicesForArtisan(artisanId)
            ) { artisan, inquiries, services ->
                Triple(artisan, inquiries, services)
            }.collect { (artisan, inquiries, services) ->
                _uiState.update {
                    it.copy(
                        artisan = artisan,
                        inquiries = inquiries,
                        services = services,
                        isLoading = false
                    )
                }
                resolveCustomerNames(inquiries)
            }
        }
    }

    /**
     * Resolves the display name of each inquiry's customer so cards show
     * who is writing. Security rules only allow single-document user reads
     * (no bulk listing), so each name is fetched individually and cached.
     */
    private fun resolveCustomerNames(inquiries: List<Inquiry>) {
        inquiries.map { it.userId }.distinct()
            .filter { it.isNotBlank() && requestedNames.add(it) }
            .forEach { userId ->
                viewModelScope.launch {
                    val name = repository.getUserById(userId).first()?.name
                    if (name.isNullOrBlank()) {
                        requestedNames.remove(userId) // retry on a later emission
                    } else {
                        _uiState.update { it.copy(customerNames = it.customerNames + (userId to name)) }
                    }
                }
            }
    }

    fun addService(name: String, price: Double) {
        viewModelScope.launch {
            val service = Service(
                id = UUID.randomUUID().toString(),
                artisanId = artisanId,
                name = name,
                price = price
            )
            repository.addService(service)
        }
    }

    /** Keeps id and artisanId — security rules only let an artisan write their own services. */
    fun updateService(service: Service, name: String, price: Double) {
        viewModelScope.launch {
            repository.updateService(service.copy(name = name, price = price))
        }
    }

    fun deleteService(serviceId: String) {
        viewModelScope.launch {
            repository.deleteService(serviceId)
        }
    }

    fun updateMyLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingLocation = true) }
            repository.updateArtisanLocationFromGps(artisanId)
                .onSuccess { (lat, lng) ->
                    _uiState.update {
                        it.copy(
                            isUpdatingLocation = false,
                            locationMessage = "Workshop pinned at your exact position (%.5f, %.5f)".format(lat, lng)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isUpdatingLocation = false,
                            locationMessage = e.message ?: "Could not update location."
                        )
                    }
                }
        }
    }

    fun clearLocationMessage() {
        _uiState.update { it.copy(locationMessage = null) }
    }

    companion object {
        fun provideFactory(
            artisanId: String,
            repository: ArtisanRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ArtisanViewModel(artisanId, repository) as T
            }
        }
    }
}
