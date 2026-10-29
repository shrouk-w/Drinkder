package com.example.drinkder.network

import com.example.drinkder.model.DrinkApiResponse
import com.example.drinkder.model.DrinkFilterResponse
import com.example.drinkder.model.IngredientListResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface DrinkApiService {
    @GET("random.php")
    suspend fun getRandomDrink(): DrinkApiResponse

    @GET("lookup.php")
    suspend fun getDrinkById(@Query("i") id: String): DrinkApiResponse

    @GET("search.php")
    suspend fun searchDrinks(@Query("s") query: String): DrinkApiResponse

    @GET("filter.php")
    suspend fun filterDrinksByIngredient(@Query("i") ingredient: String): DrinkFilterResponse

    @GET("list.php?i=list")
    suspend fun listIngredients(): IngredientListResponse

    @GET("randomselection.php")
    suspend fun getRandomSelection(): DrinkApiResponse

    @GET("popular.php")
    suspend fun getPopularDrinks(): DrinkApiResponse

    @GET("latest.php")
    suspend fun getLatestDrinks(): DrinkApiResponse
}
