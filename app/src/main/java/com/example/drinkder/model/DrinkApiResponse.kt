package com.example.drinkder.model

import com.google.gson.annotations.SerializedName

data class DrinkApiResponse(
    @SerializedName("drinks") val drinks: List<Drink>
)