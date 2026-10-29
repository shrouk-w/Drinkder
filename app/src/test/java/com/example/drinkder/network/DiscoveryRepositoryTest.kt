package com.example.drinkder.network

import com.example.drinkder.model.*
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException

class DiscoveryRepositoryTest {
    private class FakeApi : DrinkApiService {
        val calls = mutableListOf<DiscoverySource>()
        var popularFails = false
        private fun batch(source: DiscoverySource): DrinkApiResponse {
            calls += source
            if (source == DiscoverySource.POPULAR && popularFails) throw httpFailure(503)
            val drink = Gson().fromJson(
                """{"idDrink":"${source.name}","strDrink":"${source.name}","strInstructions":"Mix","strDrinkThumb":""}""",
                Drink::class.java
            )
            return DrinkApiResponse(listOf(drink))
        }
        override suspend fun getRandomSelection() = batch(DiscoverySource.RANDOM)
        override suspend fun getPopularDrinks() = batch(DiscoverySource.POPULAR)
        override suspend fun getLatestDrinks() = batch(DiscoverySource.NEW)
        override suspend fun getRandomDrink() = DrinkApiResponse(emptyList())
        override suspend fun getDrinkById(id: String) = DrinkApiResponse(emptyList())
        override suspend fun searchDrinks(query: String) = DrinkApiResponse(emptyList())
        override suspend fun filterDrinksByIngredient(ingredient: String) = DrinkFilterResponse(emptyList())
        override suspend fun listIngredients() = IngredientListResponse(emptyList())
    }

    @Test fun eachDeckUsesItsOwnEndpointAndCachesOnlyCuratedDecks() = runBlocking {
        val api = FakeApi()
        val repository = DiscoveryRepository(api, ApiRequestRunner { })
        for (source in DiscoverySource.entries) {
            assertEquals(source.name, repository.load(source).single().id)
            assertEquals(source.name, repository.load(source).single().id)
        }
        assertEquals(listOf(DiscoverySource.RANDOM, DiscoverySource.RANDOM,
            DiscoverySource.POPULAR, DiscoverySource.NEW), api.calls)
    }

    @Test fun aFailedPopularDeckCannotSilentlyBecomeRandom() = runBlocking {
        val api = FakeApi().apply { popularFails = true }
        val repository = DiscoveryRepository(api, ApiRequestRunner { })
        try {
            repository.load(DiscoverySource.POPULAR)
            fail("Expected a deck error")
        } catch (_: HttpException) { }
        assertEquals(listOf(DiscoverySource.POPULAR, DiscoverySource.POPULAR, DiscoverySource.POPULAR), api.calls)
        api.popularFails = false
        assertEquals("POPULAR", repository.load(DiscoverySource.POPULAR).single().id)
    }

    @Test fun curatedDecksRefreshAfterCacheExpiry() = runBlocking {
        val api = FakeApi()
        var clock = 0L
        val repository = DiscoveryRepository(api, ApiRequestRunner { }, now = { clock })
        repository.load(DiscoverySource.NEW)
        clock = 300_001
        repository.load(DiscoverySource.NEW)
        assertEquals(listOf(DiscoverySource.NEW, DiscoverySource.NEW), api.calls)
    }
}
