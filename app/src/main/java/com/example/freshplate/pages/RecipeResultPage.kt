package com.example.freshplate.pages

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.freshplate.API.RecipeResponse
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.freshplate.Components.IngredientItem
import com.example.freshplate.Components.LoadingLogo
import kotlinx.coroutines.delay
import androidx.compose.ui.zIndex



class ShakeDetector(
    private val threshold: Float = 20.0f, // Increased threshold to require stronger motion
    private val minShakes: Int = 2, // Require multiple shakes to trigger
    private val shakeWindow: Long = 500, // Time window to count shakes
    private val cooldown: Long = 1000, // Cooldown between triggers
    private val onShake: () -> Unit
) {
    private var lastShakeTime: Long = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var lastUpdate: Long = 0
    private var shakeCount = 0
    private var firstShakeTime: Long = 0

    fun onSensorChanged(event: SensorEvent) {
        val currentTime = System.currentTimeMillis()

        if ((currentTime - lastUpdate) > 100) { // Reduced sampling rate
            val timeDiff = currentTime - lastUpdate
            lastUpdate = currentTime

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val deltaX = x - lastX
            val deltaY = y - lastY
            val deltaZ = z - lastZ

            val acceleration = Math.sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble())

            if (acceleration > threshold) {
                if (shakeCount == 0) {
                    firstShakeTime = currentTime
                }

                shakeCount++

                // Check if we've reached required shakes within the time window
                if (shakeCount >= minShakes &&
                    (currentTime - firstShakeTime) <= shakeWindow &&
                    (currentTime - lastShakeTime) > cooldown) {
                    lastShakeTime = currentTime
                    shakeCount = 0
                    onShake()
                }
            }

            // Reset shake count if time window expired
            if (shakeCount > 0 && (currentTime - firstShakeTime) > shakeWindow) {
                shakeCount = 0
            }

            lastX = x
            lastY = y
            lastZ = z
        }
    }
}

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

    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    var lastShakeTime by remember { mutableLongStateOf(0L) }
    var showTutorial by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(4000) // Show tutorial for 4 seconds
        showTutorial = false
    }

    val publishPost = remember {
        object {
            suspend operator fun invoke() {
                try {
                    isLoading = true
                    error = null

                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser == null) {
                        error = "User not authenticated"
                        return
                    }

                    val post = hashMapOf(
                        "userId" to currentUser.uid,
                        "recipe" to recipe,
                        "userPhotoUrl" to userPhotoUrl,
                        "recipePhotoUrl" to recipe.image,
                        "timestamp" to com.google.firebase.Timestamp.now(),
                        "likes" to 0,
                        "likedBy" to listOf<String>()
                    )

                    val postRef = db.collection("posts").document()
                    postRef.set(post).await()

                    db.collection("users").document(currentUser.uid)
                        .update("posts", FieldValue.arrayUnion(postRef.id))
                        .await()

                    navController.navigate("homepage") {
                        popUpTo("homepage") { inclusive = true }
                    }
                } catch (e: Exception) {
                    error = e.message
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // Create shake detector
    val shakeDetector = remember {
        ShakeDetector(
            onShake = {
                Log.d("ShakeDetector", "Shake detected!")
                lastShakeTime = System.currentTimeMillis()
                coroutineScope.launch {
                    try {
                        publishPost()
                    } catch (e: Exception) {
                        Log.e("ShakeDetector", "Error publishing post", e)
                    }
                }
            }
        )
    }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                try {
                    shakeDetector.onSensorChanged(event)
                } catch (e: Exception) {
                    Log.e("ShakeDetector", "Error in sensor processing", e)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Use a faster sampling rate for better detection
        sensorManager.registerListener(
            listener,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME // Faster sampling rate
        )

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

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

            AnimatedVisibility(
                visible = showTutorial,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier
                    .zIndex(10f)  // Increase z-index to ensure it's on top
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                        ),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Quick Publish Feature",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Shake your phone to quickly publish your recipe!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            IconButton(
                                onClick = { showTutorial = false }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close tutorial",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
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
                            publishPost()
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
