package com.example.drinkder.network

import com.example.drinkder.model.DrinkApiResponse
import retrofit2.http.GET

interface DrinkApiService {
    @GET("random.php")
    suspend fun getRandomDrink(): DrinkApiResponse
}