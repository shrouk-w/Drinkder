package com.example.drinkder.network

import com.example.drinkder.model.Drink
import com.example.drinkder.model.DrinkApiResponse
import com.example.drinkder.model.DrinkFilterItem
import com.example.drinkder.model.DrinkFilterResponse
import com.example.drinkder.model.IngredientListResponse
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException

class CocktailSearchRepositoryTest {
    private class FakeApi : DrinkApiService {
        val filterCalls = mutableMapOf<String, Int>()
        val detailCalls = mutableMapOf<String, Int>()
        var filterFailure: (String, Int) -> Int? = { _, _ -> null }
        val brokenDetails = mutableSetOf<String>()
        private val recipes = mapOf(
            "1" to recipe("1", "Gin Fizz", "Gin"),
            "2" to recipe("2", "Vodka Soda", "Vodka"),
            "3" to recipe("3", "Mixed drink", "Gin")
        )

        override suspend fun getRandomDrink() = DrinkApiResponse(emptyList())
        override suspend fun getRandomSelection() = DrinkApiResponse(emptyList())
        override suspend fun getPopularDrinks() = DrinkApiResponse(emptyList())
        override suspend fun getLatestDrinks() = DrinkApiResponse(emptyList())
        override suspend fun listIngredients() = IngredientListResponse(emptyList())
        override suspend fun searchDrinks(query: String) = DrinkApiResponse(emptyList())
        override suspend fun getDrinkById(id: String): DrinkApiResponse {
            detailCalls[id] = (detailCalls[id] ?: 0) + 1
            if (id in brokenDetails) throw httpFailure(503)
            return DrinkApiResponse(listOfNotNull(recipes[id]))
        }
        override suspend fun filterDrinksByIngredient(ingredient: String): DrinkFilterResponse {
            val calls = (filterCalls[ingredient] ?: 0) + 1
            filterCalls[ingredient] = calls
            filterFailure(ingredient, calls)?.let { throw httpFailure(it) }
            val ids = when (ingredient) {
                "gin" -> listOf("1", "3")
                "vodka" -> listOf("2", "3")
                else -> emptyList()
            }
            return DrinkFilterResponse(ids.map { DrinkFilterItem(it, "Recipe", "") })
        }

        companion object {
            private fun recipe(id: String, name: String, ingredient: String): Drink = Gson().fromJson(
                """{"idDrink":"$id","strDrink":"$name","strInstructions":"Mix", "strDrinkThumb":"", "strIngredient1":"$ingredient"}""",
                Drink::class.java
            )
        }
    }

    private fun repository(api: FakeApi) = CocktailSearchRepository(api, ApiRequestRunner { })

    @Test fun ginAndVodkaWorkAloneAndTogetherAndReuseSuccessfulResults() = runBlocking {
        val api = FakeApi()
        val repo = repository(api)
        val gin = repo.searchPantry(listOf("gin"))
        val vodka = repo.searchPantry(listOf("vodka"))
        val both = repo.searchPantry(listOf("gin", "vodka"))
        assertEquals(setOf("1", "3"), gin.drinks.map { it.id }.toSet())
        assertEquals(setOf("2", "3"), vodka.drinks.map { it.id }.toSet())
        assertEquals(setOf("1", "2", "3"), both.drinks.map { it.id }.toSet())
        assertFalse(both.incomplete)
        assertEquals(mapOf("gin" to 1, "vodka" to 1), api.filterCalls)
    }

    @Test fun aTemporarySingleIngredientFailureIsRetried() = runBlocking {
        val api = FakeApi().apply { filterFailure = { _, calls -> if (calls == 1) 429 else null } }
        val result = repository(api).searchPantry(listOf("gin"))
        assertEquals(2, result.drinks.size)
        assertFalse(result.incomplete)
        assertEquals(2, api.filterCalls["gin"])
    }

    @Test fun aFailedIngredientIsNotReportedAsNoMatchesOrCached() = runBlocking {
        val api = FakeApi().apply { filterFailure = { _, _ -> 503 } }
        val repo = repository(api)
        try {
            repo.searchPantry(listOf("gin"))
            fail("Expected HTTP failure")
        } catch (error: HttpException) {
            assertEquals(503, error.code())
        }
        assertEquals(3, api.filterCalls["gin"])
        api.filterFailure = { _, _ -> null }
        assertEquals(2, repo.searchPantry(listOf("gin")).drinks.size)
        assertEquals(4, api.filterCalls["gin"])
    }

    @Test fun aSecondIngredientCannotHideAFailedRequest() = runBlocking {
        val api = FakeApi().apply { filterFailure = { ingredient, _ -> if (ingredient == "gin") 403 else null } }
        val result = repository(api).searchPantry(listOf("gin", "vodka"))
        assertTrue(result.drinks.isNotEmpty())
        assertTrue(result.incomplete)
    }

    @Test fun retryCompletesPartialRecipesWithoutReloadingSuccessfulOnes() = runBlocking {
        val api = FakeApi().apply { brokenDetails += "3" }
        val repo = repository(api)
        val partial = repo.searchPantry(listOf("gin"))
        assertEquals(listOf("1"), partial.drinks.map { it.id })
        assertTrue(partial.incomplete)
        api.brokenDetails.clear()
        val complete = repo.searchPantry(listOf("gin"))
        assertEquals(2, complete.drinks.size)
        assertFalse(complete.incomplete)
        assertEquals(1, api.filterCalls["gin"])
        assertEquals(1, api.detailCalls["1"])
    }

    @Test fun noNameMatchesCannotHideAnIngredientEndpointFailure() = runBlocking {
        val api = FakeApi().apply { filterFailure = { _, _ -> 403 } }
        try {
            repository(api).search("gin")
            fail("Should show a service error, not no matches")
        } catch (error: HttpException) {
            assertEquals(403, error.code())
        }
    }
}
