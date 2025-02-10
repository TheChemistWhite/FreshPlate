package com.example.freshplate.pages

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.freshplate.API.Ingredient
import com.example.freshplate.API.RecipeResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfilePage(
    navController: NavController,
    userId: String
) {
    var userName by remember { mutableStateOf("") }
    var userImage by remember { mutableStateOf("") }
    var userBio by remember { mutableStateOf("") }
    var posts by remember { mutableStateOf<List<FeedPost>>(emptyList()) }
    var followersCount by remember { mutableStateOf(0) }
    var followingCount by remember { mutableStateOf(0) }
    var isFollowing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    // Fetch user data and posts
    LaunchedEffect(userId) {
        try {
            // Get user data
            val userDoc = db.collection("users").document(userId).get().await()
            userName = "${userDoc.getString("name")} ${userDoc.getString("surname")}"
            userImage = userDoc.getString("image") ?: ""
            userBio = userDoc.getString("bio") ?: ""

            val followers = userDoc.get("followers") as? List<String> ?: emptyList()
            val following = userDoc.get("following") as? List<String> ?: emptyList()
            followersCount = followers.size
            followingCount = following.size

            // Check if current user is following
            isFollowing = currentUser?.uid?.let { uid -> followers.contains(uid) } ?: false

            // Get user posts
            val postsRef = db.collection("posts")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            posts = postsRef.documents.mapNotNull { doc ->
                try {
                    val recipeMap = doc.get("recipe") as? Map<String, Any> ?: return@mapNotNull null
                    val likedByList = doc.get("likedBy") as? List<String> ?: emptyList()

                    FeedPost(
                        id = doc.id,
                        userId = userId,
                        userName = userName,
                        userImage = userImage,
                        recipe = parseRecipeResponse(recipeMap),
                        userPhotoUrl = doc.getString("userPhotoUrl") ?: "",
                        recipePhotoUrl = doc.getString("recipePhotoUrl") ?: "",
                        timestamp = doc.getTimestamp("timestamp") ?: com.google.firebase.Timestamp.now(),
                        likes = likedByList.size,
                        likedByCurrentUser = currentUser?.uid?.let { uid -> likedByList.contains(uid) } ?: false
                    )
                } catch (e: Exception) {
                    Log.e("UserProfile", "Error processing post", e)
                    null
                }
            }
        } catch (e: Exception) {
            error = "Error loading profile: ${e.message}"
            Log.e("UserProfile", "Error loading profile", e)
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(userName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Text(
                        text = error ?: "Error loading profile",
                        color = Color.Red,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Profile Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Profile Image
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = if (userImage.isNotEmpty()) userImage else null
                                ),
                                contentDescription = "Profile Image",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(32.dp))

                            // Stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Posts count
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = posts.size.toString(),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(text = "Posts", fontSize = 14.sp)
                                }

                                // Followers count
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = followersCount.toString(),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(text = "Followers", fontSize = 14.sp)
                                }

                                // Following count
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = followingCount.toString(),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(text = "Following", fontSize = 14.sp)
                                }
                            }
                        }

                        // Bio
                        if (userBio.isNotEmpty()) {
                            Text(
                                text = userBio,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        // Follow Button (only show if not current user's profile)
                        if (currentUser?.uid != userId) {
                            Button(
                                onClick = {
                                    currentUser?.uid?.let { currentUid ->
                                        if (isFollowing) {
                                            // Unfollow
                                            db.collection("users").document(userId)
                                                .update("followers", FieldValue.arrayRemove(currentUid))
                                            db.collection("users").document(currentUid)
                                                .update("following", FieldValue.arrayRemove(userId))
                                            followersCount--
                                        } else {
                                            // Follow
                                            db.collection("users").document(userId)
                                                .update("followers", FieldValue.arrayUnion(currentUid))
                                            db.collection("users").document(currentUid)
                                                .update("following", FieldValue.arrayUnion(userId))
                                            followersCount++
                                        }
                                        isFollowing = !isFollowing
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(if (isFollowing) "Unfollow" else "Follow")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Posts Grid
                        if (posts.isNotEmpty()) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(posts) { post ->
                                    Image(
                                        painter = rememberAsyncImagePainter(post.userPhotoUrl),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .fillMaxWidth(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "No posts yet",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseRecipeResponse(recipeMap: Map<String, Any>): RecipeResponse {
    return RecipeResponse(
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
}