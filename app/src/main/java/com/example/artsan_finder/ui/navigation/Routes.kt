package com.example.artsan_finder.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Login : Route

    @Serializable
    data object Register : Route

    @Serializable
    data object Dashboard : Route

    @Serializable
    data object AdminDashboard : Route

    @Serializable
    data object ArtisanDashboard : Route

    @Serializable
    data object Discovery : Route

    @Serializable
    data object Profile : Route

    @Serializable
    data object Experiences : Route

    @Serializable
    data object Services : Route

    @Serializable
    data class ArtisanDetail(val artisanId: String) : Route

    @Serializable
    data class Chat(val inquiryId: String) : Route
}
