package com.example.drinkder.model

import com.google.gson.annotations.SerializedName

data class IngredientListResponse(val drinks: List<IngredientName>?)
data class IngredientName(@SerializedName("strIngredient1") val name: String)
