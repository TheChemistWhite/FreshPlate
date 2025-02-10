package com.example.freshplate.pages

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.freshplate.API.RecipeResponse
import com.example.freshplate.authentication.AuthViewModel
import com.example.freshplate.Components.IngredientItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class FeedPost(
    val id: String,
    val userId: String,
    val userName: String,
    val userImage: String,
    val recipe: RecipeResponse,
    val userPhotoUrl: String,
    val recipePhotoUrl: String,
    val timestamp: com.google.firebase.Timestamp,
    val likes: Int,
    val likedByCurrentUser: Boolean
)

@Composable
fun HomePage(modifier: Modifier = Modifier, authViewModel: AuthViewModel, navController: NavController) {
    val authState = authViewModel.authState.observeAsState()
    var posts by remember { mutableStateOf<List<FeedPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    // Fetch all posts
    LaunchedEffect(Unit) {
        try {
            val postsRef = db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val feedPosts = mutableListOf<FeedPost>()

            for (doc in postsRef.documents) {
                try {
                    val userId = doc.getString("userId") ?: continue
                    val userDoc = db.collection("users").document(userId).get().await()

                    val recipeMap = doc.get("recipe") as? Map<String, Any>
                    if (recipeMap != null) {
                        val recipe = RecipeResponse(
                            title = recipeMap["title"] as? String ?: "",
                            image = recipeMap["image"] as? String ?: "",
                            used_ingredients = (recipeMap["used_ingredients"] as? List<Map<String, Any>>)?.map {
                                com.example.freshplate.API.Ingredient(
                                    amount = (it["amount"] as? Number)?.toDouble() ?: 0.0,
                                    name = it["name"] as? String ?: "",
                                    unit = it["unit"] as? String ?: ""
                                )
                            } ?: emptyList(),
                            missed_ingredients = (recipeMap["missed_ingredients"] as? List<Map<String, Any>>)?.map {
                                com.example.freshplate.API.Ingredient(
                                    amount = (it["amount"] as? Number)?.toDouble() ?: 0.0,
                                    name = it["name"] as? String ?: "",
                                    unit = it["unit"] as? String ?: ""
                                )
                            } ?: emptyList()
                        )

                        val likedByList = doc.get("likedBy") as? List<String> ?: emptyList()

                        feedPosts.add(
                            FeedPost(
                                id = doc.id,
                                userId = userId,
                                userName = "${userDoc.getString("name")} ${userDoc.getString("surname")}",
                                userImage = userDoc.getString("image") ?: "",
                                recipe = recipe,
                                userPhotoUrl = doc.getString("userPhotoUrl") ?: "",
                                recipePhotoUrl = doc.getString("recipePhotoUrl") ?: "",
                                timestamp = doc.getTimestamp("timestamp") ?: com.google.firebase.Timestamp.now(),
                                likes = likedByList.size,
                                likedByCurrentUser = currentUser?.uid?.let { uid -> likedByList.contains(uid) } ?: false
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e("HomePage", "Error processing post", e)
                }
            }
            posts = feedPosts
        } catch (e: Exception) {
            error = "Error loading posts: ${e.message}"
            Log.e("HomePage", "Error loading posts", e)
        } finally {
            isLoading = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            error != null -> {
                Text(
                    text = error ?: "Error loading posts",
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
            posts.isEmpty() -> {
                Text(
                    text = "No posts yet",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
            else -> {
                LazyColumn {
                    items(posts) { post ->
                        PostCard(
                            post = post,
                            onLikeClick = { postId, currentlyLiked ->
                                if (currentUser != null) {
                                    val postRef = db.collection("posts").document(postId)
                                    if (currentlyLiked) {
                                        postRef.update("likedBy", FieldValue.arrayRemove(currentUser.uid))
                                    } else {
                                        postRef.update("likedBy", FieldValue.arrayUnion(currentUser.uid))
                                    }
                                    // Update local state
                                    posts = posts.map {
                                        if (it.id == postId) {
                                            it.copy(
                                                likedByCurrentUser = !currentlyLiked,
                                                likes = if (currentlyLiked) it.likes - 1 else it.likes + 1
                                            )
                                        } else it
                                    }
                                }
                            },
                            onUserClick = { userId ->
                                navController.navigate("user_profile/$userId")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostCard(
    post: FeedPost,
    onLikeClick: (String, Boolean) -> Unit,
    onUserClick: (String) -> Unit
) {
    var currentImageIndex by remember { mutableIntStateOf(0) }
    val images = listOf(post.userPhotoUrl, post.recipe.image)  // User photo first, then recipe image

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column {
            // User Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Avatar
                Image(
                    painter = rememberAsyncImagePainter(
                        model = if (post.userImage.isNotEmpty()) post.userImage else null
                    ),
                    contentDescription = "User avatar",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Username
                Text(
                    text = post.userName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { onUserClick(post.userId) }
                        .padding(8.dp)
                )
            }

            // Image Carousel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                // Current Image
                Image(
                    painter = rememberAsyncImagePainter(images[currentImageIndex]),
                    contentDescription = if (currentImageIndex == 0) "User photo" else "Recipe photo",
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

            // Like Button and Count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (post.likedByCurrentUser) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Like",
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onLikeClick(post.id, post.likedByCurrentUser) },
                    tint = if (post.likedByCurrentUser) Color.Red else LocalContentColor.current
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${post.likes} likes",
                    fontWeight = FontWeight.Bold
                )
            }

            // Recipe Title
            Text(
                text = post.recipe.title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            // Timestamp
            Text(
                text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(post.timestamp.toDate()),
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            // Ingredients
            Column(modifier = Modifier.padding(8.dp)) {
                if (post.recipe.used_ingredients.isNotEmpty()) {
                    Text(
                        text = "Ingredients You Have:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    post.recipe.used_ingredients.forEach { ingredient ->
                        IngredientItem(ingredient, true)
                    }
                }

                if (post.recipe.missed_ingredients.isNotEmpty()) {
                    Text(
                        text = "Missing Ingredients:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE91E63),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    post.recipe.missed_ingredients.forEach { ingredient ->
                        IngredientItem(ingredient, false)
                    }
                }
            }
        }
    }
}