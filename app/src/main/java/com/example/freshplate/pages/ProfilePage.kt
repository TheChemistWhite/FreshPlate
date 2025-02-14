package com.example.freshplate.pages

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.freshplate.API.Ingredient
import com.example.freshplate.API.RecipeResponse
import com.example.freshplate.Components.LoadingLogo
import com.example.freshplate.Components.UserPost
import com.example.freshplate.R
import com.example.freshplate.authentication.AuthState
import com.example.freshplate.authentication.AuthViewModel
import com.example.freshplate.authentication.user
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await



@Composable
fun ProfilePage(
    user: user,
    modifier: Modifier = Modifier,
    navController: NavHostController,
    authViewModel: AuthViewModel
) {

    val authState = authViewModel.authState.observeAsState()
    var userPosts by remember { mutableStateOf<List<UserPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(user.email) {
        try {
            val db = FirebaseFirestore.getInstance()
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

            Log.d("ProfilePage", "Fetching posts for user ID: $currentUserId")

            if (currentUserId != null) {
                val postsRef = db.collection("posts")
                    .whereEqualTo("userId", currentUserId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                Log.d("ProfilePage", "Found ${postsRef.documents.size} posts")

                userPosts = postsRef.documents.mapNotNull { doc ->
                    try {
                        Log.d("ProfilePage", "Processing post document: ${doc.id}")
                        Log.d("ProfilePage", "Post data: ${doc.data}")

                        val recipeMap = doc.get("recipe") as? Map<String, Any>
                        val recipe = if (recipeMap != null) {
                            RecipeResponse(
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
                        } else null

                        UserPost(
                            id = doc.id,
                            recipe = recipe ?: return@mapNotNull null,
                            userPhotoUrl = doc.getString("userPhotoUrl") ?: return@mapNotNull null,
                            recipePhotoUrl = doc.getString("recipePhotoUrl") ?: return@mapNotNull null,
                            timestamp = doc.getTimestamp("timestamp") ?: return@mapNotNull null,
                            likes = doc.getLong("likes")?.toInt() ?: 0
                        ).also {
                            Log.d("ProfilePage", "Successfully created UserPost object: $it")
                        }
                    } catch (e: Exception) {
                        Log.e("ProfilePage", "Error processing post document", e)
                        null
                    }
                }

                Log.d("ProfilePage", "Final userPosts size: ${userPosts.size}")
            }
        } catch (e: Exception) {
            Log.e("ProfilePage", "Error fetching posts", e)
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Profile Section with Image, Username and Stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Image
            val painter = if (user.image?.isEmpty() == true) {
                painterResource(id = R.drawable.baseline_person_24)
            }else {
                rememberAsyncImagePainter(model = user.image)
            }

            Image(
                painter = painter,
                contentDescription = "Profile Image",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(60.dp))

            // User Stats (Posts, Followers, Following)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = user.posts?.size.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Posts", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = user.followers?.size.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Followers", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = user.following?.size.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Following", fontSize = 14.sp)
            }
        }

        // name, surname and Bio
        Text(text = user.email.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = user.name + " " + user.surname, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = user.bio.toString(), fontSize = 16.sp)

        Spacer(modifier = Modifier.height(16.dp))

        // Edit Profile Button
        Button(
            onClick = {
                navController.navigate("update")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Edit Profile")
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Logout Button
        Button(
            onClick = {
                authViewModel.logout(user)
            },
            enabled = authState.value != AuthState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Posts",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (isLoading) {
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
        } else if (userPosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text("No posts yet")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(userPosts) { post ->
                    PostPreview(
                        post = post,
                        onPostClick = { postId ->
                            navController.navigate("post/$postId")
                        }
                    )
                }
            }
            if (BuildConfig.DEBUG) {
                Text(
                    text = "Debug Info:\n" +
                            "Loading: $isLoading\n" +
                            "Posts Count: ${userPosts.size}\n" +
                            "User ID: ${FirebaseAuth.getInstance().currentUser?.uid}",
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun PostPreview(
    post: UserPost,
    onPostClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onPostClick(post.id) }
    ) {
        Image(
            painter = rememberAsyncImagePainter(post.userPhotoUrl),
            contentDescription = "Post preview",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

