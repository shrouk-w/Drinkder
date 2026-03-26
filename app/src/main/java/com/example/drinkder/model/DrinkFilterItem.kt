package com.example.drinkder.model

import com.google.gson.annotations.SerializedName

data class DrinkFilterItem(
    @SerializedName("idDrink") val id: String,
    @SerializedName("strDrink") val name: String,
    @SerializedName("strDrinkThumb") val imageUrl: String
)
