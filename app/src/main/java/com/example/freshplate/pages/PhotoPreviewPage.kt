package com.example.freshplate.pages

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.freshplate.API.RecipeRequest
import com.example.freshplate.API.RecipeViewModel
import com.example.freshplate.API.RetrofitClient
import com.example.freshplate.Components.LoadingLogo
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun PhotoPreviewPage(
    navController: NavController,
    recipeViewModel: RecipeViewModel,
    imageUri: Uri,
    photoUrl: String
) {
    val context = LocalContext.current
    val bitmap = uriToBitmap(context, imageUri)
    val coroutineScope = rememberCoroutineScope()
    val db = Firebase.firestore

    // State for loading indicator and error handling
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(imageUri),
                contentDescription = "Photo Preview",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            isLoading = true
                            error = null

                            // Delete from Storage
                            val storage = FirebaseStorage.getInstance()
                            val storageRef = storage.getReferenceFromUrl(photoUrl)
                            try {
                                storageRef.delete().await()
                            } catch (e: Exception) {
                                Log.e("Storage", "Error deleting from storage", e)
                            }

                            // Delete from Firestore
                            try {
                                db.collection("photos")
                                    .whereEqualTo("url", photoUrl)
                                    .get()
                                    .await()
                                    .documents
                                    .forEach { doc ->
                                        doc.reference.delete()
                                    }
                            } catch (e: Exception) {
                                Log.e("Firestore", "Error deleting from Firestore", e)
                            }

                            withContext(Dispatchers.Main) {
                                navController.popBackStack()
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "An error occurred"
                            Log.e("Delete", "Error during deletion", e)
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text(text = "Discard")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            isLoading = true
                            error = null

                            // Move API call to IO dispatcher
                            val apiResponse = withContext(Dispatchers.IO) {
                                try {
                                    Log.d("API", "Making API call with URL: $photoUrl")
                                    val request = RecipeRequest(image_url = photoUrl)
                                    RetrofitClient.api.findRecipe(request)
                                } catch (e: Exception) {
                                    Log.e("API", "API call failed", e)
                                    error = when {
                                        e.message?.contains("404") == true ->
                                            "No recipe found for this image. Please try another image."
                                        e.message?.contains("timeout") == true ->
                                            "Connection timed out. Please check your internet connection and try again."
                                        else -> "Failed to analyze image: ${e.message}"
                                    }
                                    null
                                }
                            }

                            // Handle the response on the main thread
                            if (apiResponse != null && apiResponse.isNotEmpty()) {
                                val recipe = apiResponse[0]
                                // Set ViewModel data on main thread
                                recipeViewModel.setRecipe(recipe)
                                recipeViewModel.setUserPhotoUrl(photoUrl)

                                // Navigate on main thread (we're already on main thread here)
                                val encodedTitle = URLEncoder.encode(recipe.title, "UTF-8")
                                navController.navigate("recipeResult/$encodedTitle")
                            } else if (error == null) {
                                error = "No recipes found for this image. Please try another image."
                            }
                        } catch (e: Exception) {
                            Log.e("API", "Error processing API response", e)
                            error = "Error processing recipe: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading
            ) {
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
                } else {
                    Text(text = "Analyze")
                }
            }


            error?.let { errorMsg ->
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMsg,
                        color = Color.Red,
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            error = null
                        }
                    ) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

fun uriToBitmap(context: Context, imageUri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun String.encodeForNavigation(): String {
    return java.net.URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
}