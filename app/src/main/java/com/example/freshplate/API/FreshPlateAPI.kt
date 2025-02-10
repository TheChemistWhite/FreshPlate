package com.example.freshplate.API

import retrofit2.http.Body
import retrofit2.http.POST

interface FreshPlateAPI {
    @POST("findRecipe")
    suspend fun findRecipe(@Body request: RecipeRequest): List<RecipeResponse>
}