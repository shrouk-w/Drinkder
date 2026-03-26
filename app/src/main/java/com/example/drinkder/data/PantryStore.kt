package com.example.drinkder.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.pantryDataStore by preferencesDataStore(name = "pantry")

class PantryStore(private val context: Context) {
    private val keyItems = stringSetPreferencesKey("pantry_items")

    suspend fun getItemsOnce(): Set<String> {
        val prefs = context.pantryDataStore.data.first()
        return prefs[keyItems] ?: emptySet()
    }

    suspend fun setAll(items: Set<String>) {
        context.pantryDataStore.edit { prefs ->
            prefs[keyItems] = items
        }
    }
}
