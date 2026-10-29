package com.example.drinkder.network

import com.example.drinkder.model.DiscoverySource
import com.example.drinkder.model.Drink

internal class DiscoveryRepository(
    private val api: DrinkApiService,
    private val requests: ApiRequestRunner = ApiRequestRunner(),
    private val now: () -> Long = { System.nanoTime() / 1_000_000 }
) {
    private data class Cached(val drinks: List<Drink>, val expiresAt: Long)
    private val cache = mutableMapOf<DiscoverySource, Cached>()

    suspend fun load(source: DiscoverySource): List<Drink> {
        if (source != DiscoverySource.RANDOM) {
            cache[source]?.takeIf { it.expiresAt > now() }?.let { return it.drinks }
        }
        val drinks = requests.run {
            when (source) {
                DiscoverySource.RANDOM -> api.getRandomSelection()
                DiscoverySource.POPULAR -> api.getPopularDrinks()
                DiscoverySource.NEW -> api.getLatestDrinks()
            }
        }.drinks.orEmpty().distinctBy { it.id }
        check(drinks.isNotEmpty()) { "The selected deck contains no drinks" }
        if (source != DiscoverySource.RANDOM) cache[source] = Cached(drinks, now() + 5 * 60_000)
        return drinks
    }
}
