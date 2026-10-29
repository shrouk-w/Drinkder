package com.example.drinkder.ui.searchpage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.drinkder.data.PantryStore
import com.example.drinkder.data.IngredientCatalogStore
import com.example.drinkder.network.ApiRequestRunner
import com.example.drinkder.model.Drink
import com.example.drinkder.network.RetrofitInstance
import com.example.drinkder.network.CocktailSearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

class SearchPageViewModel(application: Application) : AndroidViewModel(application) {
    enum class SearchMode { QUERY, PANTRY }

    enum class AlcoholFilter {
        ALL,
        ALCOHOLIC,
        NON_ALCOHOLIC
    }

    private val pantryStore = PantryStore(application)
    private val catalogStore = IngredientCatalogStore(application)
    val ingredientNames = MutableLiveData<List<String>>(emptyList())
    val controlsExpanded = MutableLiveData(true)

    private val _results = MutableLiveData<List<Drink>>(emptyList())
    val results: LiveData<List<Drink>> = _results

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _message = MutableLiveData("Search for a cocktail or ingredient, e.g. Margarita or gin.")
    val message: LiveData<String> = _message

    private val _pantryItems = MutableLiveData<List<String>>(emptyList())
    val pantryItems: LiveData<List<String>> = _pantryItems

    private val _alcoholFilter = MutableLiveData(AlcoholFilter.ALL)
    val alcoholFilter: LiveData<AlcoholFilter> = _alcoholFilter

    private val _searchMode = MutableLiveData(SearchMode.QUERY)
    val searchMode: LiveData<SearchMode> = _searchMode
    private val _matchMode = MutableLiveData(MatchMode.EXPLORE)
    val matchMode: LiveData<MatchMode> = _matchMode
    val hasError = MutableLiveData(false)

    private var searchJob: Job? = null
    private var lastQuery: String = ""
    private var rawResults: List<Drink> = emptyList()
    private val repository by lazy { CocktailSearchRepository(RetrofitInstance.api) }
    val warning = MutableLiveData<String?>(null)

    init {
        viewModelScope.launch {
            _pantryItems.value = pantryStore.getItemsOnce().sorted()
            updateVisibleResults()
        }
        viewModelScope.launch {
            try {
                val cached = catalogStore.read()
                ingredientNames.value = cached.names
                if (cached.names.isEmpty() || System.currentTimeMillis() - cached.updatedAt > 86_400_000) {
                    val names = ApiRequestRunner().run { RetrofitInstance.api.listIngredients() }
                        .drinks.orEmpty().map { it.name.trim() }.filter(String::isNotBlank)
                        .distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
                    if (names.isNotEmpty()) {
                        ingredientNames.value = names
                        catalogStore.save(names)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Catalogue availability must not block manually entered ingredients.
            }
        }
    }

    fun search(query: String) {
        val normalizedQuery = query.trim()
        lastQuery = normalizedQuery

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            hasError.value = false
            warning.value = null
            rawResults = emptyList()
            _results.value = emptyList()
            _message.value = ""

            runCatching {
                if (_searchMode.value == SearchMode.PANTRY) {
                    repository.searchPantry(_pantryItems.value.orEmpty())
                } else if (normalizedQuery.isBlank()) {
                    CocktailSearchRepository.SearchResult(emptyList())
                } else {
                    repository.search(normalizedQuery)
                }
            }.onSuccess { result ->
                rawResults = result.drinks
                updateVisibleResults()
                if (result.incomplete) warning.value = "Some recipes could not be loaded. Retry to complete the results."
            }.onFailure { error ->
                if (error is CancellationException) throw error
                if (com.example.drinkder.BuildConfig.DEBUG) {
                    val status = (error as? retrofit2.HttpException)?.code()
                    android.util.Log.w("DrinkSearch", "Search failure: ${error.javaClass.simpleName}; HTTP=$status")
                }
                hasError.value = true
                rawResults = emptyList()
                _results.value = emptyList()
                _message.value = when ((error as? retrofit2.HttpException)?.code()) {
                    429 -> "The cocktail service is busy. Wait a moment and try again."
                    401, 403 -> "The cocktail service refused this request. Please try again shortly."
                    in 500..599 -> "The cocktail service is temporarily unavailable. Please try again."
                    else -> "Could not load recipes. Check your connection and try again."
                }
            }

            _isLoading.value = false
        }
    }

    fun retry() {
        search(lastQuery)
    }

    fun setAlcoholFilter(filter: AlcoholFilter) {
        _alcoholFilter.value = filter
        if (_isLoading.value != true) updateVisibleResults()
    }

    fun setSearchMode(mode: SearchMode) {
        if (_searchMode.value == mode) return
        _searchMode.value = mode
        search(lastQuery)
    }

    fun setMatchMode(mode: MatchMode) {
        if (_matchMode.value == mode) return
        _matchMode.value = mode
        if (_isLoading.value != true) updateVisibleResults()
    }

    fun addPantryIngredient(value: String) {
        val normalized = value.trim().lowercase()
        if (normalized.isBlank()) return

        val updated = ((_pantryItems.value ?: emptyList()) + normalized)
            .distinct()
            .sorted()

        if (updated == _pantryItems.value) return
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
        if (_searchMode.value == SearchMode.PANTRY) {
            search(lastQuery)
        }
    }

    private fun updateVisibleResults() {
        val pantry = (_pantryItems.value ?: emptyList()).map { it.lowercase() }.toSet()

        val pantryMode = _searchMode.value == SearchMode.PANTRY
        var visible = rawResults.filter(::matchesAlcoholFilter)

        if (pantryMode) {
            visible = visible
                .map { drink -> drink to pantryScore(drink, pantry) }
                .filter { (_, score) ->
                    if (_matchMode.value == MatchMode.EXACT) score.complete
                    else score.matched > 0
                }
                .sortedWith(
                    compareByDescending<Pair<Drink, PantryMatcher.Score>> { it.second.matched }
                        .thenByDescending { scoreRatio(it.second) }
                        .thenBy { it.second.total - it.second.matched }
                        .thenBy { it.first.name }
                )
                .map { it.first }
        } else if (_matchMode.value == MatchMode.EXACT) {
            visible = visible.filter { drink ->
                PantryMatcher.matchesExactQuery(drink.name, drinkIngredients(drink), lastQuery)
            }
        }

        _results.value = visible
        _message.value = when {
            pantryMode && pantry.isEmpty() -> "Add an ingredient to your bar to find recipes."
            !pantryMode && lastQuery.isBlank() -> defaultMessage()
            rawResults.isEmpty() && pantryMode -> emptyPantryMessage()
            rawResults.isEmpty() -> emptyMessage(lastQuery)
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

    private fun pantryScore(drink: Drink, pantry: Set<String>) = PantryMatcher.score(
        drinkIngredients(drink), pantry, _matchMode.value ?: MatchMode.EXPLORE
    )

    private fun scoreRatio(score: PantryMatcher.Score): Float =
        if (score.total == 0) 0f else score.matched.toFloat() / score.total

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

    private fun defaultMessage(): String {
        return "Search for a cocktail or ingredient, e.g. Margarita or gin."
    }

    private fun emptyPantryMessage(): String {
        return "No pantry matches yet. Add ingredients to get suggestions."
    }

    private fun emptyMessage(query: String): String {
        return "No drinks matched \"$query\" by name or ingredient."
    }

    private fun filteredOutMessage(): String {
        return if (_searchMode.value == SearchMode.PANTRY) {
            if (_matchMode.value == MatchMode.EXACT) {
                "No exact matches with these ingredients and filters. Try Explore or add an ingredient."
            } else {
                "No ingredient matches under the current filter."
            }
        } else {
            "Results exist, but none fit the current filter."
        }
    }
}
