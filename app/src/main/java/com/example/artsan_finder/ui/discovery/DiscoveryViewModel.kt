package com.example.artsan_finder.ui.discovery

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artsan_finder.data.model.Artisan
import com.example.artsan_finder.data.model.Category
import com.example.artsan_finder.data.repository.ArtisanRepository
import com.example.artsan_finder.utils.LocationProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DiscoveryUiState(
    val artisans: List<ArtisanWithDistance> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,
    val minRating: Float = 0f,
    val userLocation: Location? = null
)

data class ArtisanWithDistance(
    val artisan: Artisan,
    val distanceKm: Float? = null
)

class DiscoveryViewModel(
    private val repository: ArtisanRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _minRating = MutableStateFlow(0f)
    private val _userLocation = MutableStateFlow<Location?>(null)
    private val _isLoading = MutableStateFlow(false)

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<DiscoveryUiState> = combine(
        repository.artisans,
        repository.categories,
        _searchQuery,
        _selectedCategoryId,
        _minRating,
        _userLocation,
        _isLoading
    ) { args ->
        val artisans = args[0] as List<Artisan>
        val categories = args[1] as List<Category>
        val query = args[2] as String
        val categoryId = args[3] as String?
        val rating = args[4] as Float
        val location = args[5] as Location?
        val isLoading = args[6] as Boolean

        val filtered = artisans.filter { artisan ->
            val matchesQuery = artisan.name.contains(query, ignoreCase = true) ||
                    artisan.bio.contains(query, ignoreCase = true)
            val matchesCategory = categoryId == null || artisan.categoryId == categoryId
            val matchesRating = artisan.rating >= rating
            matchesQuery && matchesCategory && matchesRating
        }.map { artisan ->
            val distance = location?.let {
                locationProvider.calculateDistance(
                    it.latitude, it.longitude,
                    artisan.latitude, artisan.longitude
                )
            }
            ArtisanWithDistance(artisan, distance)
        }.sortedBy { it.distanceKm ?: Float.MAX_VALUE }

        DiscoveryUiState(
            artisans = filtered,
            categories = categories,
            searchQuery = query,
            selectedCategoryId = categoryId,
            minRating = rating,
            userLocation = location,
            isLoading = isLoading
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiscoveryUiState())

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.refreshCategories()
            repository.refreshArtisans()
            _isLoading.value = false
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onCategorySelect(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun onRatingFilterApply(rating: Float) {
        _minRating.value = rating
    }

    fun updateLocation() {
        viewModelScope.launch {
            _userLocation.value = locationProvider.getCurrentLocation()
        }
    }

    companion object {
        fun provideFactory(
            repository: ArtisanRepository,
            locationProvider: LocationProvider
        ): androidx.lifecycle.ViewModelProvider.Factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return DiscoveryViewModel(repository, locationProvider) as T
            }
        }
    }
}
