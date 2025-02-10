package com.example.freshplate.pages

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.freshplate.API.Ingredient
import com.example.freshplate.API.RecipeResponse
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.freshplate.Components.IngredientItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeResultPage(
    navController: NavController,
    recipe: RecipeResponse,
    userPhotoUrl: String
) {
    // State variables
    var currentImageIndex by remember { mutableIntStateOf(0) }
    val images = listOf(recipe.image, userPhotoUrl)
    val coroutineScope = rememberCoroutineScope()
    val db = Firebase.firestore
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recipe Found") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(bottom = 80.dp)
                    .background(Color.White)
            ) {
                // Image Carousel
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        // Current Image
                        Image(
                            painter = rememberAsyncImagePainter(images[currentImageIndex]),
                            contentDescription = if (currentImageIndex == 0) recipe.title else "User uploaded photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Navigation Arrows
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Arrow
                            IconButton(
                                onClick = {
                                    if (currentImageIndex > 0) currentImageIndex--
                                },
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous image",
                                    tint = Color.White
                                )
                            }

                            // Right Arrow
                            IconButton(
                                onClick = {
                                    if (currentImageIndex < images.size - 1) currentImageIndex++
                                },
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next image",
                                    tint = Color.White
                                )
                            }
                        }

                        // Image Indicators
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            images.forEachIndexed { index, _ ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(8.dp)
                                        .background(
                                            color = if (currentImageIndex == index)
                                                Color.White
                                            else
                                                Color.White.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }

                // Recipe Title
                item {
                    Text(
                        text = recipe.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Ingredients Section
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Ingredients You Have:",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        recipe.used_ingredients.forEach { ingredient ->
                            IngredientItem(ingredient, true)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Missing Ingredients:",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE91E63),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        recipe.missed_ingredients.forEach { ingredient ->
                            IngredientItem(ingredient, false)
                        }
                    }
                }
            }

            // Action Buttons at the bottom
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Discard Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                isLoading = true
                                error = null

                                // Delete from Storage
                                val storage = FirebaseStorage.getInstance()
                                val storageRef = storage.getReferenceFromUrl(userPhotoUrl)
                                try {
                                    storageRef.delete().await()
                                } catch (e: Exception) {
                                    // Log error but continue
                                }

                                // Delete from Firestore
                                try {
                                    db.collection("photos")
                                        .whereEqualTo("imageUrl", userPhotoUrl)
                                        .get()
                                        .await()
                                        .documents
                                        .forEach { doc ->
                                            doc.reference.delete()
                                        }
                                } catch (e: Exception) {
                                    // Log error but continue
                                }

                                navController.navigate("homepage") {
                                    popUpTo("homepage") { inclusive = true }
                                }
                            } catch (e: Exception) {
                                error = e.message
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("Discard")
                }

                // Publish Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                isLoading = true
                                error = null

                                val currentUser = FirebaseAuth.getInstance().currentUser
                                if (currentUser == null) {
                                    error = "User not authenticated"
                                    return@launch
                                }

                                // Create post document
                                val post = hashMapOf(
                                    "userId" to currentUser.uid,
                                    "recipe" to recipe,
                                    "userPhotoUrl" to userPhotoUrl,
                                    "recipePhotoUrl" to recipe.image,
                                    "timestamp" to com.google.firebase.Timestamp.now(),
                                    "likes" to 0,
                                    "likedBy" to listOf<String>()  // Initialize empty likes array
                                )

                                Log.d("PostCreation", "Creating post with data: $post")
                                Log.d("PostCreation", "Current user ID: ${currentUser.uid}")

                                // Add post to Firestore
                                val postRef = db.collection("posts").document()
                                postRef.set(post).await()

                                Log.d("PostCreation", "Post created with ID: ${postRef.id}")

                                // Update user's posts array
                                db.collection("users").document(currentUser.uid)
                                    .update("posts", FieldValue.arrayUnion(postRef.id))
                                    .await()

                                Log.d("PostCreation", "User posts array updated")

                                navController.navigate("homepage") {
                                    popUpTo("homepage") { inclusive = true }
                                }
                            } catch (e: Exception) {
                                error = e.message
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("Publish")
                }
            }

            // Loading Overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            // Error Dialog
            error?.let { errorMessage ->
                AlertDialog(
                    onDismissRequest = { error = null },
                    title = { Text("Error") },
                    text = { Text(errorMessage) },
                    confirmButton = {
                        TextButton(onClick = { error = null }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}