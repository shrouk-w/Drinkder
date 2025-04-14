package com.example.drinkder.model

import com.google.gson.annotations.SerializedName

data class Drink(
    @SerializedName("idDrink") val id: String,
    @SerializedName ("strDrink") val name: String,
    @SerializedName ("strInstructions") val description: String,
    @SerializedName ("strDrinkThumb") val imageUrl: String
)