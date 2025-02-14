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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.freshplate.Components.IngredientItem
import com.example.freshplate.Components.LoadingLogo
import com.example.freshplate.Components.UserPost
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailPage(
    navController: NavController,
    postId: String
) {
    var post by remember { mutableStateOf<UserPost?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var currentImageIndex by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()

    // Fetch post details
    LaunchedEffect(postId) {
        try {
            val postDoc = db.collection("posts").document(postId).get().await()

            Log.d("PostDetail", "Raw post data: ${postDoc.data}")

            try {
                // Extract recipe data
                val recipeMap = postDoc.get("recipe") as? Map<String, Any>
                if (recipeMap == null) {
                    error = "Invalid recipe data"
                    return@LaunchedEffect
                }

                // Parse recipe data
                val recipe = RecipeResponse(
                    title = recipeMap["title"] as? String ?: "",
                    image = recipeMap["image"] as? String ?: "",
                    used_ingredients = (recipeMap["used_ingredients"] as? List<Map<String, Any>>)?.map {
                        Ingredient(
                            amount = (it["amount"] as? Number)?.toDouble() ?: 0.0,
                            name = it["name"] as? String ?: "",
                            unit = it["unit"] as? String ?: ""
                        )
                    } ?: emptyList(),
                    missed_ingredients = (recipeMap["missed_ingredients"] as? List<Map<String, Any>>)?.map {
                        Ingredient(
                            amount = (it["amount"] as? Number)?.toDouble() ?: 0.0,
                            name = it["name"] as? String ?: "",
                            unit = it["unit"] as? String ?: ""
                        )
                    } ?: emptyList()
                )

                post = UserPost(
                    id = postDoc.id,
                    recipe = recipe,
                    userPhotoUrl = postDoc.getString("userPhotoUrl") ?: "",
                    recipePhotoUrl = postDoc.getString("recipePhotoUrl") ?: "",
                    timestamp = postDoc.getTimestamp("timestamp") ?: com.google.firebase.Timestamp.now(),
                    likes = postDoc.getLong("likes")?.toInt() ?: 0
                )

                Log.d("PostDetail", "Successfully parsed post: $post")
            } catch (e: Exception) {
                Log.e("PostDetail", "Error parsing post data", e)
                error = "Error parsing post data: ${e.message}"
            }
        } catch (e: Exception) {
            Log.e("PostDetail", "Error loading post", e)
            error = "Error loading post: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Delete Post Function
    fun deletePost() {
        coroutineScope.launch {
            try {
                isLoading = true
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    error = "User not authenticated"
                    return@launch
                }

                // Get current user's document
                val userDoc = db.collection("users").document(currentUser.uid).get().await()
                val currentPosts = userDoc.get("posts") as? List<String> ?: emptyList()

                // Create new posts list without the deleted post
                val updatedPosts = currentPosts.filter { it != postId }

                // Update user document with new posts list
                db.collection("users").document(currentUser.uid)
                    .update("posts", updatedPosts)
                    .await()

                // Delete the post document
                db.collection("posts").document(postId).delete().await()

                // Navigate back to profile
                navController.navigate("profile") {
                    popUpTo("profile") { inclusive = true }
                }
            } catch (e: Exception) {
                error = "Error deleting post: ${e.message}"
                Log.e("PostDetail", "Error deleting post", e)
            } finally {
                isLoading = false
                showDeleteConfirmation = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingLogo(
                            modifier = Modifier
                                .size(300.dp)
                                .background(Color.Black)
                        )
                    }
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error ?: "Error loading post",
                            color = Color.Red,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(
                            onClick = { navController.popBackStack() }
                        ) {
                            Text("Go Back")
                        }
                    }
                }
                post != null -> {
                    val images = listOf(post?.userPhotoUrl, post?.recipe?.image)

                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 80.dp)
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
                                        contentDescription = if (currentImageIndex == 0)
                                            "User uploaded photo" else "Recipe photo",
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
                                    text = post?.recipe?.title ?: "",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                            // Ingredients
                            item {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Ingredients You Have:",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    post?.recipe?.used_ingredients?.forEach { ingredient ->
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
                                    post?.recipe?.missed_ingredients?.forEach { ingredient ->
                                        IngredientItem(ingredient, false)
                                    }
                                }
                            }
                        }

                        // Delete Button
                        Button(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            )
                        ) {
                            Text("Delete Post")
                        }
                    }
                }
            }

            // Delete Confirmation Dialog
            if (showDeleteConfirmation) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmation = false },
                    title = { Text("Delete Post") },
                    text = { Text("Are you sure you want to delete this post?") },
                    confirmButton = {
                        TextButton(
                            onClick = { deletePost() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.Red
                            )
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmation = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}