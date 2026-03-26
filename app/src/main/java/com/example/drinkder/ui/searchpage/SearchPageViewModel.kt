package com.example.drinkder.ui.searchpage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.drinkder.data.PantryStore
import com.example.drinkder.model.Drink
import com.example.drinkder.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchPageViewModel(application: Application) : AndroidViewModel(application) {

    enum class AlcoholFilter {
        ALL,
        ALCOHOLIC,
        NON_ALCOHOLIC
    }

    private data class PantryScore(
        val matched: Int,
        val total: Int
    )

    private val pantryStore = PantryStore(application)

    private val _results = MutableLiveData<List<Drink>>(emptyList())
    val results: LiveData<List<Drink>> = _results

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _message = MutableLiveData("Add pantry items or search by drink name or ingredient.")
    val message: LiveData<String> = _message

    private val _pantryItems = MutableLiveData<List<String>>(emptyList())
    val pantryItems: LiveData<List<String>> = _pantryItems

    private val _alcoholFilter = MutableLiveData(AlcoholFilter.ALL)
    val alcoholFilter: LiveData<AlcoholFilter> = _alcoholFilter

    private val _pantryOnly = MutableLiveData(false)
    val pantryOnly: LiveData<Boolean> = _pantryOnly

    private var searchJob: Job? = null
    private var lastQuery: String = ""
    private var rawResults: List<Drink> = emptyList()

    init {
        viewModelScope.launch {
            _pantryItems.value = pantryStore.getItemsOnce().sorted()
            updateVisibleResults()
        }
    }

    fun search(query: String) {
        val normalizedQuery = query.trim()
        lastQuery = normalizedQuery

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            _message.value = ""

            runCatching {
                if (normalizedQuery.isBlank()) {
                    searchFromPantry()
                } else {
                    searchByName(normalizedQuery)
                }
            }.onSuccess { drinks ->
                rawResults = drinks
                updateVisibleResults()
            }.onFailure {
                rawResults = emptyList()
                _results.value = emptyList()
                _message.value = "Search failed. Check your connection and try again."
            }

            _isLoading.value = false
        }
    }

    fun retry() {
        search(lastQuery)
    }

    fun setAlcoholFilter(filter: AlcoholFilter) {
        _alcoholFilter.value = filter
        updateVisibleResults()
    }

    fun setPantryOnly(enabled: Boolean) {
        _pantryOnly.value = enabled
        updateVisibleResults()
    }

    fun addPantryIngredient(value: String) {
        val normalized = value.trim().lowercase()
        if (normalized.isBlank()) return

        val updated = ((_pantryItems.value ?: emptyList()) + normalized)
            .distinct()
            .sorted()

        _pantryItems.value = updated
        persistPantry(updated)
        refreshIfPantryDriven()
    }

    fun removePantryIngredient(value: String) {
        val updated = (_pantryItems.value ?: emptyList())
            .filterNot { it.equals(value, ignoreCase = true) }

        _pantryItems.value = updated
        persistPantry(updated)
        refreshIfPantryDriven()
    }

    private fun persistPantry(items: List<String>) {
        viewModelScope.launch {
            pantryStore.setAll(items.toSet())
        }
    }

    private fun refreshIfPantryDriven() {
        if (lastQuery.isBlank()) {
            search("")
        } else {
            updateVisibleResults()
        }
    }

    private fun updateVisibleResults() {
        val pantry = (_pantryItems.value ?: emptyList()).map { it.lowercase() }.toSet()

        val baseResults = if (_pantryOnly.value == true && lastQuery.isNotBlank()) {
            rawResults.filter { drink ->
                pantryScore(drink, pantry).matched > 0
            }
        } else {
            rawResults
        }

        var visible = baseResults.filter(::matchesAlcoholFilter)

        if (_pantryOnly.value == true) {
            visible = visible
                .map { drink -> drink to pantryScore(drink, pantry) }
                .filter { (_, score) -> score.matched > 0 }
                .sortedWith(
                    compareByDescending<Pair<Drink, PantryScore>> { it.second.matched }
                        .thenByDescending { scoreRatio(it.second) }
                        .thenBy { it.second.total - it.second.matched }
                        .thenBy { it.first.name }
                )
                .map { it.first }
        }

        _results.value = visible
        _message.value = when {
            lastQuery.isBlank() && pantry.isEmpty() -> defaultMessage()
            rawResults.isEmpty() && lastQuery.isBlank() -> emptyPantryMessage()
            rawResults.isEmpty() -> emptyMessage(lastQuery)
            visible.isEmpty() && _pantryOnly.value == true && pantry.isEmpty() -> pantryNeededMessage()
            visible.isEmpty() -> filteredOutMessage()
            else -> ""
        }
    }

    private fun matchesAlcoholFilter(drink: Drink): Boolean {
        return when (_alcoholFilter.value ?: AlcoholFilter.ALL) {
            AlcoholFilter.ALL -> true
            AlcoholFilter.ALCOHOLIC -> drink.alcoholic?.contains("Alcoholic", ignoreCase = true) == true &&
                drink.alcoholic?.contains("Non", ignoreCase = true) != true
            AlcoholFilter.NON_ALCOHOLIC -> drink.alcoholic?.contains("Non alcoholic", ignoreCase = true) == true ||
                drink.alcoholic?.contains("Non Alcoholic", ignoreCase = true) == true
        }
    }

    private fun pantryScore(drink: Drink, pantry: Set<String>): PantryScore {
        val ingredients = drinkIngredients(drink)
        val matched = ingredients.count { ingredient ->
            pantry.any { pantryItem ->
                ingredient.contains(pantryItem) || pantryItem.contains(ingredient)
            }
        }
        return PantryScore(matched = matched, total = ingredients.size.coerceAtLeast(1))
    }

    private fun scoreRatio(score: PantryScore): Float = score.matched.toFloat() / score.total.toFloat()

    private fun drinkIngredients(drink: Drink): List<String> {
        return listOf(
            drink.ingredient1,
            drink.ingredient2,
            drink.ingredient3,
            drink.ingredient4,
            drink.ingredient5,
            drink.ingredient6,
            drink.ingredient7,
            drink.ingredient8,
            drink.ingredient9,
            drink.ingredient10,
            drink.ingredient11,
            drink.ingredient12,
            drink.ingredient13,
            drink.ingredient14,
            drink.ingredient15
        ).mapNotNull { it?.trim()?.lowercase()?.takeIf(String::isNotBlank) }
    }

    private suspend fun searchByName(query: String): List<Drink> = withContext(Dispatchers.IO) {
        runCatching { RetrofitInstance.api.searchDrinks(query).drinks.orEmpty() }.getOrDefault(emptyList())
    }

    private suspend fun searchFromPantry(): List<Drink> = withContext(Dispatchers.IO) {
        val pantry = (_pantryItems.value ?: emptyList())
        if (pantry.isEmpty()) return@withContext emptyList()

        pantry.map { ingredient ->
            async {
                searchByIngredient(ingredient)
            }
        }.flatMap { it.await() }
            .distinctBy { it.id }
    }

    private suspend fun searchByIngredient(query: String): List<Drink> = withContext(Dispatchers.IO) {
        val filtered = RetrofitInstance.api.filterDrinksByIngredient(query).drinks.orEmpty()
        filtered.map { item ->
            async {
                runCatching { RetrofitInstance.api.getDrinkById(item.id).drinks?.firstOrNull() }.getOrNull()
            }
        }.mapNotNull { it.await() }
    }

    private fun defaultMessage(): String {
        return "Add pantry items or search by drink name or ingredient."
    }

    private fun emptyPantryMessage(): String {
        return "No pantry matches yet. Add ingredients to get suggestions."
    }

    private fun emptyMessage(query: String): String {
        return "No drinks matched \"$query\" by name or ingredient."
    }

    private fun pantryNeededMessage(): String {
        return "Turn off pantry mode or add more pantry ingredients."
    }

    private fun filteredOutMessage(): String {
        return if (_pantryOnly.value == true) {
            "No good pantry matches under the current filter."
        } else {
            "Results exist, but none fit the current filter."
        }
    }
}
