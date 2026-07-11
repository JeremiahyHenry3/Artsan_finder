package com.example.artsan_finder.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore by preferencesDataStore(name = "app_prefs")

/**
 * LocalStorageManager - Centralized local storage management
 * Handles all local data persistence including session, user profile, and app settings.
 * This design makes it easy to migrate to Firebase later by simply swapping the backend.
 */
class LocalStorageManager(private val context: Context) {

    private val moshi = Moshi.Builder().build()

    // Preference Keys
    private val SESSION_USER_ID_KEY = stringPreferencesKey("session_user_id")
    private val SESSION_USER_EMAIL_KEY = stringPreferencesKey("session_user_email")
    private val SESSION_USER_NAME_KEY = stringPreferencesKey("session_user_name")
    private val REMEMBER_ME_KEY = booleanPreferencesKey("remember_me")
    private val LAST_LOGIN_TIME_KEY = longPreferencesKey("last_login_time")
    private val APP_THEME_KEY = stringPreferencesKey("app_theme")
    private val NOTIFICATION_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
    private val OFFLINE_MODE_KEY = booleanPreferencesKey("offline_mode")

    // ==================== SESSION MANAGEMENT ====================

    /**
     * Saves user session to local storage
     */
    suspend fun saveUserSession(
        userId: String,
        userEmail: String,
        userName: String,
        rememberMe: Boolean = false
    ) {
        context.appDataStore.edit { prefs ->
            prefs[SESSION_USER_ID_KEY] = userId
            prefs[SESSION_USER_EMAIL_KEY] = userEmail
            prefs[SESSION_USER_NAME_KEY] = userName
            prefs[REMEMBER_ME_KEY] = rememberMe
            prefs[LAST_LOGIN_TIME_KEY] = System.currentTimeMillis()
        }
    }

    /**
     * Get current user ID from session
     */
    val currentUserId: Flow<String?> = context.appDataStore.data.map { prefs ->
        prefs[SESSION_USER_ID_KEY]
    }

    /**
     * Get current user email from session
     */
    val currentUserEmail: Flow<String?> = context.appDataStore.data.map { prefs ->
        prefs[SESSION_USER_EMAIL_KEY]
    }

    /**
     * Get current user name from session
     */
    val currentUserName: Flow<String?> = context.appDataStore.data.map { prefs ->
        prefs[SESSION_USER_NAME_KEY]
    }

    /**
     * Get remember me preference
     */
    val rememberMeEnabled: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[REMEMBER_ME_KEY] ?: false
    }

    /**
     * Get last login time
     */
    val lastLoginTime: Flow<Long?> = context.appDataStore.data.map { prefs ->
        prefs[LAST_LOGIN_TIME_KEY]
    }

    /**
     * Clear user session (logout)
     */
    suspend fun clearUserSession() {
        context.appDataStore.edit { prefs ->
            prefs.remove(SESSION_USER_ID_KEY)
            prefs.remove(SESSION_USER_EMAIL_KEY)
            prefs.remove(SESSION_USER_NAME_KEY)
            prefs.remove(LAST_LOGIN_TIME_KEY)
        }
    }

    /**
     * Check if user is logged in
     */
    val isUserLoggedIn: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[SESSION_USER_ID_KEY] != null
    }

    // ==================== APP PREFERENCES ====================

    /**
     * Save app theme preference
     */
    suspend fun setAppTheme(theme: String) {
        context.appDataStore.edit { prefs ->
            prefs[APP_THEME_KEY] = theme
        }
    }

    /**
     * Get app theme preference
     */
    val appTheme: Flow<String> = context.appDataStore.data.map { prefs ->
        prefs[APP_THEME_KEY] ?: "light"
    }

    /**
     * Set notification preference
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[NOTIFICATION_ENABLED_KEY] = enabled
        }
    }

    /**
     * Get notification preference
     */
    val notificationsEnabled: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[NOTIFICATION_ENABLED_KEY] ?: true
    }

    /**
     * Set offline mode
     */
    suspend fun setOfflineMode(enabled: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[OFFLINE_MODE_KEY] = enabled
        }
    }

    /**
     * Get offline mode status
     */
    val offlineMode: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[OFFLINE_MODE_KEY] ?: false
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Clear all local storage
     */
    suspend fun clearAllData() {
        context.appDataStore.edit { it.clear() }
    }

    /**
     * Get all current preferences as a map
     */
    fun getAllPreferences(): Flow<Map<String, Any>> = context.appDataStore.data.map { prefs ->
        mapOf(
            "user_id" to (prefs[SESSION_USER_ID_KEY] ?: ""),
            "user_email" to (prefs[SESSION_USER_EMAIL_KEY] ?: ""),
            "user_name" to (prefs[SESSION_USER_NAME_KEY] ?: ""),
            "remember_me" to (prefs[REMEMBER_ME_KEY] ?: false),
            "last_login_time" to (prefs[LAST_LOGIN_TIME_KEY] ?: 0L),
            "app_theme" to (prefs[APP_THEME_KEY] ?: "light"),
            "notifications_enabled" to (prefs[NOTIFICATION_ENABLED_KEY] ?: true),
            "offline_mode" to (prefs[OFFLINE_MODE_KEY] ?: false)
        )
    }
}

