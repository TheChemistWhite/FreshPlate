package com.example.freshplate.API

data class RecipeRequest(
    val image_url: String
)

data class Ingredient(
    val amount: Double,
    val name: String,
    val unit: String
)

data class RecipeResponse(
    val image: String,
    val missed_ingredients: List<Ingredient>,
    val title: String,
    val used_ingredients: List<Ingredient>
)