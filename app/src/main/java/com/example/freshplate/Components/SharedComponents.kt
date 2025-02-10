package com.example.freshplate.Components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.freshplate.API.Ingredient
import com.example.freshplate.API.RecipeResponse

@Composable
fun IngredientItem(ingredient: Ingredient, isAvailable: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFE91E63),
                    shape = CircleShape
                )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "${ingredient.amount} ${ingredient.unit} ${ingredient.name}",
            fontSize = 16.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

data class UserPost(
    val id: String,
    val recipe: RecipeResponse,
    val userPhotoUrl: String,
    val recipePhotoUrl: String,
    val timestamp: com.google.firebase.Timestamp,
    val likes: Int
)
