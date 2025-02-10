package com.example.freshplate.API

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RecipeViewModel : ViewModel() {
    private val _currentRecipe = MutableStateFlow<RecipeResponse?>(null)
    val currentRecipe: StateFlow<RecipeResponse?> = _currentRecipe

    private val _userPhotoUrl = MutableStateFlow<String?>(null)
    val userPhotoUrl: StateFlow<String?> = _userPhotoUrl

    fun setRecipe(recipe: RecipeResponse) {
        _currentRecipe.value = recipe
    }

    fun setUserPhotoUrl(url: String) {
        _userPhotoUrl.value = url
    }
}