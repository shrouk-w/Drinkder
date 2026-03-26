package com.example.drinkder.model

import com.google.gson.annotations.SerializedName

data class DrinkFilterResponse(
    @SerializedName("drinks") val drinks: List<DrinkFilterItem>?
)
