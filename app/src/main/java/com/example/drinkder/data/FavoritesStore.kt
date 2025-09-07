package com.example.drinkder.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "favorites")

class FavoritesStore(private val context: Context) {
    private val KEY_IDS = stringSetPreferencesKey("favorite_ids")

    suspend fun getIdsOnce(): Set<String> {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_IDS] ?: emptySet()
    }

    suspend fun setAll(ids: Set<String>) {
        context.dataStore.edit { it[KEY_IDS] = ids }
    }

    suspend fun add(id: String) {
        context.dataStore.edit { prefs ->
            val set = (prefs[KEY_IDS] ?: emptySet()).toMutableSet()
            set.add(id)
            prefs[KEY_IDS] = set
        }
    }

    suspend fun remove(id: String) {
        context.dataStore.edit { prefs ->
            val set = (prefs[KEY_IDS] ?: emptySet()).toMutableSet()
            set.remove(id)
            prefs[KEY_IDS] = set
        }
    }
}
