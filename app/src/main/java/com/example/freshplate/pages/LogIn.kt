package com.example.freshplate.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.freshplate.R

@Composable
fun LogIn() {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
                contentDescription = "Fresh Plate",
                modifier = Modifier
                    .size(200.dp) // Adjust the size of the image
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
                    // Email input field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp)) // Space between fields

                    // Password input field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation() // Hide password characters
                    )

                    Spacer(modifier = Modifier.height(32.dp)) // Space before buttons

                    // Login button
                    Button(
                        onClick = { /* Handle login */ },
                        modifier = Modifier.fillMaxWidth() // Make the button stretch across the card width
                    ) {
                        Text("Log In", fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp)) // Space between Login and Sign Up button

                    // Sign Up button
                    Button(
                        onClick = { /* Handle Sign Up */ },
                        modifier = Modifier.fillMaxWidth() // Make the button stretch across the card width
                    ) {
                        Text("Sign Up", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun LogInPreview() {
    LogIn()
}
