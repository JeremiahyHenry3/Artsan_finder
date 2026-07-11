package com.example.artsan_finder.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.cacheDataStore by preferencesDataStore(name = "app_cache")

/**
 * LocalCacheManager - Manages app-level caching for better performance
 * Stores search history, recent views, and other cache data locally.
 * This cache can be easily synced with Firebase later.
 */
class LocalCacheManager(private val context: Context) {

    private val moshi = Moshi.Builder().build()

    // Cache Keys
    private val SEARCH_HISTORY_KEY = stringSetPreferencesKey("search_history")
    private val RECENT_VIEWS_KEY = stringPreferencesKey("recent_views")
    private val FAVORITE_ARTISANS_KEY = stringSetPreferencesKey("favorite_artisans")
    private val SYNC_STATUS_KEY = stringPreferencesKey("sync_status")
    private val LAST_SYNC_TIME_KEY = stringPreferencesKey("last_sync_time")

    // ==================== SEARCH HISTORY ====================

    /**
     * Add a search term to history
     */
    suspend fun addToSearchHistory(searchTerm: String) {
        context.cacheDataStore.edit { prefs ->
            val history = prefs[SEARCH_HISTORY_KEY]?.toMutableSet() ?: mutableSetOf()
            history.add(searchTerm)
            // Keep only last 20 searches
            if (history.size > 20) {
                history.remove(history.first())
            }
            prefs[SEARCH_HISTORY_KEY] = history
        }
    }

    /**
     * Get search history
     */
    val searchHistory: Flow<Set<String>> = context.cacheDataStore.data.map { prefs ->
        prefs[SEARCH_HISTORY_KEY] ?: emptySet()
    }

    /**
     * Clear search history
     */
    suspend fun clearSearchHistory() {
        context.cacheDataStore.edit { prefs ->
            prefs.remove(SEARCH_HISTORY_KEY)
        }
    }

    // ==================== FAVORITE ARTISANS ====================

    /**
     * Add artisan to favorites
     */
    suspend fun addToFavorites(artisanId: String) {
        context.cacheDataStore.edit { prefs ->
            val favorites = prefs[FAVORITE_ARTISANS_KEY]?.toMutableSet() ?: mutableSetOf()
            favorites.add(artisanId)
            prefs[FAVORITE_ARTISANS_KEY] = favorites
        }
    }

    /**
     * Remove artisan from favorites
     */
    suspend fun removeFromFavorites(artisanId: String) {
        context.cacheDataStore.edit { prefs ->
            val favorites = prefs[FAVORITE_ARTISANS_KEY]?.toMutableSet() ?: mutableSetOf()
            favorites.remove(artisanId)
            prefs[FAVORITE_ARTISANS_KEY] = favorites
        }
    }

    /**
     * Get all favorites
     */
    val favoriteArtisans: Flow<Set<String>> = context.cacheDataStore.data.map { prefs ->
        prefs[FAVORITE_ARTISANS_KEY] ?: emptySet()
    }

    /**
     * Check if artisan is favorite
     */
    fun isArtisanFavorite(artisanId: String): Flow<Boolean> = context.cacheDataStore.data.map { prefs ->
        prefs[FAVORITE_ARTISANS_KEY]?.contains(artisanId) ?: false
    }

    /**
     * Clear all favorites
     */
    suspend fun clearFavorites() {
        context.cacheDataStore.edit { prefs ->
            prefs.remove(FAVORITE_ARTISANS_KEY)
        }
    }

    // ==================== RECENT VIEWS ====================

    /**
     * Save recent views (JSON format)
     */
    suspend fun saveRecentViews(recentViewsJson: String) {
        context.cacheDataStore.edit { prefs ->
            prefs[RECENT_VIEWS_KEY] = recentViewsJson
        }
    }

    /**
     * Get recent views
     */
    val recentViews: Flow<String?> = context.cacheDataStore.data.map { prefs ->
        prefs[RECENT_VIEWS_KEY]
    }

    /**
     * Clear recent views
     */
    suspend fun clearRecentViews() {
        context.cacheDataStore.edit { prefs ->
            prefs.remove(RECENT_VIEWS_KEY)
        }
    }

    // ==================== SYNC STATUS ====================

    /**
     * Set sync status (for future Firebase integration)
     */
    suspend fun setSyncStatus(status: String) {
        context.cacheDataStore.edit { prefs ->
            prefs[SYNC_STATUS_KEY] = status
            prefs[LAST_SYNC_TIME_KEY] = System.currentTimeMillis().toString()
        }
    }

    /**
     * Get sync status
     */
    val syncStatus: Flow<String?> = context.cacheDataStore.data.map { prefs ->
        prefs[SYNC_STATUS_KEY]
    }

    /**
     * Get last sync time
     */
    val lastSyncTime: Flow<String?> = context.cacheDataStore.data.map { prefs ->
        prefs[LAST_SYNC_TIME_KEY]
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Clear all cache
     */
    suspend fun clearAllCache() {
        context.cacheDataStore.edit { it.clear() }
    }

    /**
     * Get cache stats
     */
    fun getCacheStats(): Flow<Map<String, String>> = context.cacheDataStore.data.map { prefs ->
        mapOf(
            "search_history_count" to (prefs[SEARCH_HISTORY_KEY]?.size?.toString() ?: "0"),
            "favorites_count" to (prefs[FAVORITE_ARTISANS_KEY]?.size?.toString() ?: "0"),
            "sync_status" to (prefs[SYNC_STATUS_KEY] ?: "not_synced"),
            "last_sync_time" to (prefs[LAST_SYNC_TIME_KEY] ?: "never")
        )
    }
}

