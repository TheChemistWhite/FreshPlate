package com.example.freshplate.pages

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.freshplate.Camera.Camera
import com.example.freshplate.R
import com.example.freshplate.authentication.AuthState
import com.example.freshplate.authentication.AuthViewModel
import com.example.freshplate.authentication.user
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@Composable
fun UpdatePage(
    user: user,
    modifier: Modifier = Modifier,
    navController: NavHostController,
    authViewModel: AuthViewModel
) {

    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var selectedImageUri: Uri? by remember { mutableStateOf(null) }
    var showProgressDialog by remember { mutableStateOf(false) }

    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val storage = FirebaseStorage.getInstance()
    val storageRef = storage.reference

    // Gallery launcher
    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                coroutineScope.launch {
                    // Upload the image to Firebase Storage
                    try {
                        val fileName = "profile_images/${UUID.randomUUID()}.jpg"
                        val imageRef = storageRef.child(fileName)
                        val uploadTask = imageRef.putFile(it).await()
                        val previousImageUrl = user.image

                        // Get download URL
                        val downloadUrl = imageRef.downloadUrl.await()

                        // Save URL in Firestore
                        user.image = downloadUrl.toString()
                        showProgressDialog = true
                        if (!previousImageUrl.isNullOrEmpty()) {
                            try {
                                val previousImageRef = storage.getReferenceFromUrl(previousImageUrl)
                                previousImageRef.delete().await()
                                showProgressDialog = false
                                Toast.makeText(
                                    context,
                                    "Image uploaded successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Failed to delete previous image: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Image upload failed: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF005b61)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Image at the top
            Image(
                painter = painterResource(id = R.drawable.freshplate),
                contentDescription = "FreshPlate",
                modifier = Modifier
                    .size(300.dp)
                    .padding(top = 6.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(0.dp))

            // Card to wrap inputs and buttons
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    //Profile image with icon
                    val painter = if (user.image?.isEmpty() == true) {
                        painterResource(id = R.drawable.baseline_person_24)
                    } else {
                        rememberAsyncImagePainter(model = user.image)
                    }

                    Image(
                        painter = painter,
                        contentDescription = "Profile Image",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                            .clickable {
                                galleryLauncher.launch("image/*")
                            },

                        contentScale = ContentScale.Crop
                    )
                    name = user.name.toString()
                    // Name input field with icon
                    OutlinedTextField(
                        value = name,
                        onValueChange = { user.name = it; name = it },
                        label = { Text("Name") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = "Name Icon"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    surname = user.surname.toString()
                    // Surname input field with icon
                    OutlinedTextField(
                        value = surname,
                        onValueChange = { user.surname = it; surname = it },
                        label = { Text("Surname") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = "Surname Icon"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    bio = user.bio.toString()
                    // Bio input field with icon
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { user.bio = it; bio = it },
                        label = { Text("Bio") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    username = user.username.toString()
                    // Username input field with icon
                    OutlinedTextField(
                        value = username,
                        onValueChange = { user.username = it; username = it },
                        label = { Text("Username") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = "Surname Icon"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Update button
                    Button(
                        onClick = {
                            keyboard?.hide()
                            coroutineScope.launch {
                                authViewModel.update(user)
                                navController.navigate("profile")
                            }
                        },
                        enabled = authState.value != AuthState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Update", fontSize = 18.sp)
                    }
                }
            }
        }
        if (showProgressDialog) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 4.dp,
                modifier = Modifier.wrapContentSize(Alignment.Center).size(50.dp)
            )
        }
    }
}