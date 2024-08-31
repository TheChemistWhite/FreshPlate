package com.example.freshplate.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.freshplate.R
import com.example.freshplate.authentication.AuthState
import com.example.freshplate.authentication.AuthViewModel
import com.example.freshplate.authentication.user
import kotlinx.coroutines.launch

@Composable
fun UpdatePage(user: user, modifier: Modifier = Modifier, navController: NavHostController, authViewModel: AuthViewModel){

    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    val authState = authViewModel.authState.observeAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope() // Define coroutine scope

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp) // Add padding to the whole screen
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
                    .size(100.dp) // Adjust the size of the image
                    .padding(top = 16.dp), // Optional padding from the top
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp)) // Space between image and card

            // Card to wrap inputs and buttons
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), // Adding elevation to give a card look
                colors = CardDefaults.cardColors(containerColor = Color.White) // Card background set to white
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp) // Padding inside the card
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    //Profile image with icon
                    Image(
                        painter = if (user.image?.isEmpty() == true)
                            painterResource(id = R.drawable.baseline_person_24)
                        else painterResource(id = R.drawable.baseline_person_24),
                        contentDescription = "Profile Image",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.Gray),
                        contentScale = ContentScale.Crop
                    )
                    name = user.name.toString()
                    // Name input field with icon
                    OutlinedTextField(
                        value = name,
                        onValueChange = { user.name = it; name = it },
                        label = { Text("Name") },
                        leadingIcon = { Icon(
                            Icons.Filled.Person,
                            contentDescription = "Name Icon") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp)) // Space between fields

                    surname = user.surname.toString()
                    // Surname input field with icon
                    OutlinedTextField(
                        value = surname,
                        onValueChange = { user.surname = it; surname = it },
                        label = { Text("Surname") },
                        leadingIcon = { Icon(
                            Icons.Filled.Person,
                            contentDescription = "Surname Icon") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp)) // Space between fields

                    bio = user.bio.toString()
                    // Bio input field with icon
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { user.bio = it; bio = it },
                        label = { Text("Bio") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp)) // Space between fields

                    username = user.username.toString()
                    // Username input field with icon
                    OutlinedTextField(
                        value = username,
                        onValueChange = { user.username = it; username = it },
                        label = { Text("Username") },
                        leadingIcon = { Icon(
                            Icons.Filled.Person,
                            contentDescription = "Surname Icon") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(32.dp)) // Space before buttons

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
                        modifier = Modifier.fillMaxWidth() // Make the button stretch across the card width
                    ) {
                        Text("Update", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}