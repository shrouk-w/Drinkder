package com.example.drinkder.network

import com.example.drinkder.model.Drink
import com.example.drinkder.model.DrinkFilterItem
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

internal class CocktailSearchRepository(
    private val api: DrinkApiService,
    private val requests: ApiRequestRunner = ApiRequestRunner(),
    private val now: () -> Long = { System.nanoTime() / 1_000_000 }
) {
    data class SearchResult(val drinks: List<Drink>, val incomplete: Boolean = false)
    private data class Cached<T>(val value: T, val expiresAt: Long)

    private val drinks = ConcurrentHashMap<String, Drink>()
    private val filters = ConcurrentHashMap<String, Cached<List<DrinkFilterItem>>>()
    private val ingredients = ConcurrentHashMap<String, Cached<SearchResult>>()

    suspend fun search(query: String): SearchResult = supervisorScope {
        val name = async { capture {
            val found = requests.run { api.searchDrinks(query) }.drinks.orEmpty()
            found.forEach { drinks[it.id] = it }
            SearchResult(found)
        } }
        val ingredient = async { capture { searchIngredient(query) } }
        combine(listOf(name.await(), ingredient.await()))
    }

    suspend fun searchPantry(items: List<String>): SearchResult = supervisorScope {
        val batches = items.map { it.trim().lowercase() }.filter(String::isNotBlank).distinct()
            .map { ingredient -> async { capture { searchIngredient(ingredient) } } }
            .awaitAll()
        combine(batches)
    }

    private suspend fun searchIngredient(query: String): SearchResult = supervisorScope {
        val key = query.trim().lowercase()
        ingredients[key]?.takeIf { it.expiresAt > now() }?.let { return@supervisorScope it.value }

        val ids = filters[key]?.takeIf { it.expiresAt > now() }?.value
            ?: requests.run { api.filterDrinksByIngredient(key) }.drinks.orEmpty()
                .also { filters[key] = Cached(it, now() + CACHE_MS) }

        val recipes = ids.distinctBy { it.id }.map { item -> async {
            capture {
                val drink = drinks[item.id] ?: requests.run { api.getDrinkById(item.id) }
                    .drinks?.firstOrNull()?.also { drinks[it.id] = it }
                    ?: error("The cocktail service returned no recipe for a listed drink")
                SearchResult(listOf(drink))
            }
        } }.awaitAll()

        val result = combine(recipes)
        // Never cache a failed or incomplete ingredient search as an empty result.
        if (!result.incomplete) ingredients[key] = Cached(result, now() + CACHE_MS)
        result
    }

    private fun combine(batches: List<Result<SearchResult>>): SearchResult {
        val successful = batches.mapNotNull { it.getOrNull() }
        val found = successful.flatMap { it.drinks }.distinctBy { it.id }
        val failure = batches.firstNotNullOfOrNull { it.exceptionOrNull() }
        if (found.isEmpty() && failure != null) throw failure
        return SearchResult(found, failure != null || successful.any { it.incomplete })
    }

    private suspend fun <T> capture(block: suspend () -> T): Result<T> = runCatching {
        block()
    }.onFailure { if (it is CancellationException) throw it }

    private companion object {
        const val CACHE_MS = 5 * 60 * 1_000L
    }
}
