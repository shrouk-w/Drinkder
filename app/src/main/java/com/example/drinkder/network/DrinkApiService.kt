package com.example.drinkder.network

import com.example.drinkder.model.DrinkApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface DrinkApiService {
    @GET("random.php")
    suspend fun getRandomDrink(): DrinkApiResponse

    @GET("lookup.php")
    suspend fun getDrinkById(@Query("i") id: String): DrinkApiResponse
}