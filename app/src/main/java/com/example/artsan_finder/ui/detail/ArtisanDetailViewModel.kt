package com.example.artsan_finder.ui.detail

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

data class ArtisanDetailUiState(
    val artisan: Artisan? = null,
    val services: List<Service> = emptyList(),
    val isLoading: Boolean = false,
    val inquirySent: Boolean = false,
    val error: String? = null
)

class ArtisanDetailViewModel(
    private val artisanId: String,
    private val currentUserId: String,
    private val repository: ArtisanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtisanDetailUiState(isLoading = true))
    val uiState: StateFlow<ArtisanDetailUiState> = _uiState.asStateFlow()

    init {
        loadArtisanDetails()
    }

    private fun loadArtisanDetails() {
        viewModelScope.launch {
            try {
                // Attempt to refresh services from remote; if remote or DB has none, fall back to sample services
                try {
                    repository.refreshServices(artisanId)
                } catch (_: Exception) { /* ignore */ }

                // Observe the artisan and services reactively so any update
                // (e.g. admin editing the profile or location) is reflected immediately.
                combine(
                    repository.observeArtisanById(artisanId),
                    repository.getServicesForArtisan(artisanId)
                ) { artisan, services ->
                    val finalArtisan = artisan ?: getFallbackArtisan(artisanId)
                    val finalServices = if (services.isEmpty()) getFallbackServicesForArtisan(artisanId) else services
                    _uiState.update {
                        it.copy(
                            artisan = finalArtisan,
                            services = finalServices,
                            isLoading = false
                        )
                    }
                }.collect()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // Fallback data in case DB is empty or stale (keeps detail screen working when navigating from in-memory samples)
    private fun getFallbackArtisan(id: String): Artisan? {
        return when (id) {
            "art_1" -> Artisan("art_1", "Juma Hassan", "cat_1", 4.97f, -1.286389, 36.817223, "+255 654 123 001", "juma.hassan@plumbing.com", "Expert plumber with 15+ years of professional experience. Specialized in emergency plumbing repairs, bathroom installations, and modern piping systems. Available 24/7 for urgent repairs.", "https://i.pinimg.com/736x/6d/66/af/6d66af4d10a9a7d19d1df880b0ce3b23.jpg", true)
            "art_2" -> Artisan("art_2", "Mary M.", "cat_2", 4.93f, -1.2921, 36.8219, "+255 654 123 002", "mary.electric@services.com", "Certified electrician with 12 years of industry experience. Expert in residential and commercial electrical installations, smart home wiring, solar panels, and safety inspections.", "https://images.unsplash.com/photo-1544724569-5f546fd6f2b5?q=80&w=2070&auto=format&fit=crop", true)
            "art_3" -> Artisan("art_3", "David L.", "cat_3", 4.87f, -1.2800, 36.8100, "+255 654 123 003", "david.carpenter@crafts.com", "Modern carpenter specializing in custom furniture, kitchen cabinets, and home renovations. Uses sustainable materials and provides consultation for all carpentry projects.", "https://images.unsplash.com/photo-1589939705384-5185137a7f0f?q=80&w=2070&auto=format&fit=crop", true)
            "art_4" -> Artisan("art_4", "Sarah K.", "cat_5", 4.69f, -1.3000, 36.8300, "+255 654 123 004", "sarah.painting@pro.com", "Professional painter with 8 years of experience in interior and exterior painting. Specializes in wall art, textured finishes, and premium paint applications for residential and commercial spaces.", "https://i.pinimg.com/1200x/01/78/a1/0178a10b5c4fba3512c66e4c950c4ffd.jpg", true)
            "art_5" -> Artisan("art_5", "Amos P.", "cat_1", 4.98f, -1.2700, 36.8000, "+255 654 123 005", "amos.emergency@24-7.com", "24/7 emergency plumber specializing in leak detection and emergency repairs. Quick response time with guaranteed professional service for all plumbing emergencies.", "https://i.pinimg.com/736x/42/4d/66/424d66f9d433eb170b5e14badfce7950.jpg", true)
            "art_6" -> Artisan("art_6", "Grace J.", "cat_2", 4.95f, -1.2600, 36.7900, "+255 654 123 006", "grace.rapid@electric.com", "Rapid response electrical technician for emergency situations. Available immediately for urgent electrical issues, outages, and safety hazards. Licensed and fully insured.", "https://i.pinimg.com/1200x/00/ce/ab/00ceabcf6b336b1a9e2a22eb971baa9b.jpg", true)
            "art_7" -> Artisan("art_7", "Kelvin T.", "cat_1", 4.88f, -1.2550, 36.7850, "+255 654 123 007", "kelvin.drain@urgent.com", "Specialized in urgent drain clearing and pipe maintenance. Uses advanced equipment for fast, efficient drain cleaning without disrupting your home. Emergency callouts available.", "https://i.pinimg.com/1200x/b6/54/f3/b654f3336a2ccec52ed2d78346903541.jpg", true)
            "art_8" -> Artisan("art_8", "Peter O.", "cat_3", 4.91f, -1.2500, 36.7800, "+255 654 123 008", "peter.furniture@custom.com", "Expert in custom furniture design and fabrication. Provides free consultations for all projects. Known for unique designs, quality craftsmanship, and attention to detail in every piece.", "https://i.pinimg.com/736x/dc/81/43/dc8143a0f8c5b060aa51a8b0119bdbe9.jpg", true)
            "art_9" -> Artisan("art_9", "Aisha S.", "cat_5", 4.85f, -1.2450, 36.7750, "+255 654 123 009", "aisha.interior@design.com", "Interior painting specialist with 10+ years delivering premium finishes. Transform your spaces with professional color consultation, decorative techniques, and flawless execution.", "https://images.unsplash.com/photo-1562259949-e8e7689d7828?q=80&w=2070&auto=format&fit=crop", true)
            "art_10" -> Artisan("art_10", "Bakari", "cat_3", 4.79f, -1.2400, 36.7700, "+255 654 123 010", "bakari.kitchen@remodel.com", "Kitchen remodeling specialist with expertise in modern designs and functional layouts. Project-based pricing with transparent quotes. Delivers high-quality results on time and on budget.", "https://images.unsplash.com/photo-1556911220-e15b29be8c8f?q=80&w=2070&auto=format&fit=crop", true)
            else -> null
        }
    }

    private fun getFallbackServicesForArtisan(id: String): List<Service> {
        return when (id) {
            "art_1" -> listOf(Service("ser_1", "art_1", "Emergency Leak Repair", 85.0), Service("ser_2", "art_1", "Bathroom Installation", 250.0))
            "art_2" -> listOf(Service("ser_6", "art_2", "Home Wiring Inspection", 100.0), Service("ser_7", "art_2", "Solar Panel Installation", 1200.0))
            "art_3" -> listOf(Service("ser_11", "art_3", "Custom Furniture Design", 450.0), Service("ser_12", "art_3", "Kitchen Cabinet Installation", 800.0))
            "art_4" -> listOf(Service("ser_16", "art_4", "Interior Painting", 350.0), Service("ser_17", "art_4", "Exterior Painting", 500.0))
            "art_5" -> listOf(Service("ser_21", "art_5", "24/7 Emergency Repair", 200.0))
            "art_6" -> listOf(Service("ser_25", "art_6", "24/7 Emergency Electrical", 250.0))
            "art_7" -> listOf(Service("ser_29", "art_7", "Urgent Drain Clearing", 150.0))
            "art_8" -> listOf(Service("ser_33", "art_8", "Custom Wardrobe Design", 650.0))
            "art_9" -> listOf(Service("ser_37", "art_9", "Premium Interior Painting", 450.0))
            "art_10" -> listOf(Service("ser_41", "art_10", "Full Kitchen Remodel", 3500.0))
            else -> emptyList()
        }
    }

    fun sendInquiry(message: String) {
        viewModelScope.launch {
            val inquiry = Inquiry(
                id = UUID.randomUUID().toString(),
                userId = currentUserId, // repository re-stamps this from live Firebase Auth
                artisanId = artisanId,
                message = message,
                status = "pending"
            )
            repository.sendInquiry(inquiry)
                .onSuccess { _uiState.update { it.copy(inquirySent = true) } }
                .onFailure { e ->
                    // A refused inquiry must never look sent — the customer would
                    // be chatting into a thread the artisan can never see.
                    _uiState.update { it.copy(error = e.message ?: "Could not send the inquiry") }
                }
        }
    }

    fun resetInquiryStatus() {
        _uiState.update { it.copy(inquirySent = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        fun provideFactory(
            artisanId: String,
            currentUserId: String,
            repository: ArtisanRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ArtisanDetailViewModel(artisanId, currentUserId, repository) as T
            }
        }
    }
}
