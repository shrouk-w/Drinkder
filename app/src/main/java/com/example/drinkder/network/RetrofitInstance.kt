package com.example.drinkder.network

import com.example.drinkder.BuildConfig
import com.example.drinkder.model.DrinkFilterResponse
import com.example.drinkder.model.DrinkFilterItem
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import retrofit2.Converter
import java.lang.reflect.Type
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    val api: DrinkApiService by lazy { createApi("v1") }
    val discoveryApi: DrinkApiService by lazy { createApi("v2") }

    private fun createApi(version: String): DrinkApiService {
        val apiKey = BuildConfig.COCKTAIL_API_KEY
        check(apiKey.isNotBlank()) {
            "Missing TheCocktailDB API_KEY. Set API_KEY in the root .env file and rebuild the app."
        }
        val gson = GsonBuilder()
            .registerTypeAdapter(DrinkFilterResponse::class.java,
                JsonDeserializer<DrinkFilterResponse> { json, _, context ->
                    val drinks = json.asJsonObject.get("drinks")
                    DrinkFilterResponse(
                        if (drinks != null && drinks.isJsonArray) {
                            context.deserialize(drinks, object : TypeToken<List<DrinkFilterItem>>() {}.type)
                        } else {
                            emptyList()
                        }
                    )
                })
            .create()
        return Retrofit.Builder()
            .baseUrl("https://www.thecocktaildb.com/api/json/$version/$apiKey/")
            .addConverterFactory(object : Converter.Factory() {
                override fun responseBodyConverter(
                    type: Type, annotations: Array<Annotation>, retrofit: Retrofit
                ): Converter<ResponseBody, *>? {
                    if (type != DrinkFilterResponse::class.java) return null
                    return Converter<ResponseBody, DrinkFilterResponse> { body ->
                        body.use {
                            val payload = it.string()
                            if (payload.isBlank()) DrinkFilterResponse(emptyList())
                            else gson.fromJson(payload, DrinkFilterResponse::class.java)
                        }
                    }
                }
            })
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(DrinkApiService::class.java)
    }
}
