package com.pdf.alldocument.office.pdfreaderapp.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_SORT_ORDER = stringPreferencesKey("sort_order")
        val KEY_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val KEY_NIGHT_MODE = booleanPreferencesKey("night_mode")
        val KEY_HORIZONTAL_SCROLL = booleanPreferencesKey("horizontal_scroll")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_IS_GRID_VIEW = booleanPreferencesKey("is_grid_view")
        val KEY_PRIVACY_POLICY_ACCEPTED = booleanPreferencesKey("privacy_policy_accepted")
        val KEY_IS_LANGUAGE_SELECTED = booleanPreferencesKey("is_language_selected")
    }

    val themeModeFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_THEME] ?: "SYSTEM"
    }

    val sortOrderFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_SORT_ORDER] ?: "DATE_DESC"
    }

    val languageFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_LANGUAGE] ?: "en"
    }

    val keepScreenOnFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_KEEP_SCREEN_ON] ?: false
    }

    val nightModeFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_NIGHT_MODE] ?: false
    }

    val horizontalScrollFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_HORIZONTAL_SCROLL] ?: false
    }

    val onboardingCompletedFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val isGridViewFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_IS_GRID_VIEW] ?: false
    }

    val privacyPolicyAcceptedFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_PRIVACY_POLICY_ACCEPTED] ?: false
    }

    val isLanguageSelectedFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_IS_LANGUAGE_SELECTED] ?: false
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME] = mode
        }
    }

    suspend fun setSortOrder(order: String) {
        dataStore.edit { preferences ->
            preferences[KEY_SORT_ORDER] = order
        }
    }

    suspend fun setLanguage(code: String) {
        dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = code
            preferences[KEY_IS_LANGUAGE_SELECTED] = true
        }
    }

    suspend fun setIsLanguageSelected(selected: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_IS_LANGUAGE_SELECTED] = selected
        }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_KEEP_SCREEN_ON] = enabled
        }
    }

    suspend fun setNightMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_NIGHT_MODE] = enabled
        }
    }

    suspend fun setHorizontalScroll(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_HORIZONTAL_SCROLL] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setGridView(isGrid: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_IS_GRID_VIEW] = isGrid
        }
    }

    suspend fun setPrivacyPolicyAccepted(accepted: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_PRIVACY_POLICY_ACCEPTED] = accepted
        }
    }
}
