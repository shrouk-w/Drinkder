package com.example.drinkder.model

import com.google.gson.annotations.SerializedName

data class Drink(
    @SerializedName("idDrink") val id: String,
    @SerializedName ("strDrink") val name: String,
    @SerializedName ("strInstructions") val description: String,
    @SerializedName ("strDrinkThumb") val imageUrl: String,
    @SerializedName ("strIngredient1") val ingredient1: String?,
    @SerializedName ("strIngredient2") val ingredient2: String?,
    @SerializedName ("strIngredient3") val ingredient3: String?,
    @SerializedName ("strIngredient4") val ingredient4: String?,
    @SerializedName ("strIngredient5") val ingredient5: String?,
    @SerializedName ("strIngredient6") val ingredient6: String?,
    @SerializedName ("strIngredient7") val ingredient7: String?,
    @SerializedName ("strIngredient8") val ingredient8: String?,
    @SerializedName ("strIngredient9") val ingredient9: String?,
    @SerializedName ("strIngredient10") val ingredient10: String?,
    @SerializedName ("strIngredient11") val ingredient11: String?,
    @SerializedName ("strIngredient12") val ingredient12: String?,
    @SerializedName ("strIngredient13") val ingredient13: String?,
    @SerializedName ("strIngredient14") val ingredient14: String?,
    @SerializedName ("strIngredient15") val ingredient15: String?,

)