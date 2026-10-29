package com.example.drinkder.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.ingredientCatalog by preferencesDataStore(name = "ingredient_catalog")

class IngredientCatalogStore(private val context: Context) {
    data class Catalog(val names: List<String>, val updatedAt: Long)
    private val namesKey = stringSetPreferencesKey("names")
    private val updatedKey = longPreferencesKey("updated_at")

    suspend fun read(): Catalog {
        val preferences = context.ingredientCatalog.data.first()
        return Catalog(preferences[namesKey].orEmpty().sorted(), preferences[updatedKey] ?: 0)
    }

    suspend fun save(names: List<String>) {
        context.ingredientCatalog.edit {
            it[namesKey] = names.toSet()
            it[updatedKey] = System.currentTimeMillis()
        }
    }
}
