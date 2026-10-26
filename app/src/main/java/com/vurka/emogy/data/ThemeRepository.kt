package com.vurka.emogy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

class ThemeRepository(private val context: Context) {
    private val dataStore = context.dataStore

    companion object {
        private val SELECTED_THEME_ID_KEY = stringPreferencesKey("selected_theme_id")
    }

    val selectedThemeId: Flow<String> = dataStore.data.map { prefs ->
        prefs[SELECTED_THEME_ID_KEY] ?: "default"
    }

    suspend fun setSelectedThemeId(id: String) {
        dataStore.edit { it[SELECTED_THEME_ID_KEY] = id }
    }
}
